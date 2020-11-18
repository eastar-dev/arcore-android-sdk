/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.helloar

import android.content.DialogInterface
import android.log.Log
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.examples.java.common.helpers.*
import com.google.ar.core.examples.java.common.samplerender.*
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer
import com.google.ar.core.examples.java.helloar.databinding.ActivityMainBinding
import com.google.ar.core.exceptions.*
import smart.base.BActivity
import java.io.IOException
import java.util.*

/** 이것은 ARCore API를 사용하여 증강 현실 (AR) 애플리케이션을 만드는 방법을 보여주는 간단한 예입니다. 애플리케이션은 감지 된 모든 평면을 표시하고 사용자가 평면을 탭하여 Android 로봇의 3D 모델을 배치 할 수 있도록합니다. */
open class HelloArActivity : BActivity(), SampleRender.Renderer {
    private lateinit var bb: ActivityMainBinding

    private var installRequested = false
    private var session: Session? = null
    private val messageSnackbarHelper = SnackbarHelper()
    private lateinit var displayRotationHelper: DisplayRotationHelper
    private val trackingStateHelper = TrackingStateHelper(this)
    private var tapHelper: TapHelper? = null
    private var render: SampleRender? = null
    private var depthTexture: Texture? = null
    private var calculateUVTransform = true
    private var planeRenderer: PlaneRenderer? = null
    private var backgroundRenderer: BackgroundRenderer? = null
    private var hasSetTextureNames = false
    private val depthSettings = DepthSettings()
    private val depthSettingsMenuDialogCheckboxes = BooleanArray(2)
    private val instantPlacementSettings = InstantPlacementSettings()
    private val instantPlacementSettingsMenuDialogCheckboxes = BooleanArray(1)
    private var pointCloudVertexBuffer: VertexBuffer? = null

    private lateinit var pointCloudMesh: Mesh
    private lateinit var pointCloudShader: Shader

    // 포인트 클라우드가 변경되지 않은 경우 VBO가 업데이트되지 않도록 렌더링 된 마지막 포인트 클라우드를 추적합니다. PointCloud 개체를 비교할 수 없으므로 타임 스탬프를 사용하여이 작업을 수행합니다.
    private var lastPointCloudTimestamp: Long = 0
    private lateinit var virtualObjectMesh: Mesh
    private var virtualObjectShader: Shader? = null
    private var virtualObjectDepthShader: Shader? = null

    // 주어진 색상으로 개체를 배치하는 데 사용되는 탭으로 만든 앵커입니다.
    private class ColoredAnchor(val anchor: Anchor, var color: FloatArray, val trackable: Trackable)

    private val anchors = ArrayList<ColoredAnchor>()

    // 각 프레임에 대한 할당 수를 줄이기 위해 여기에 할당 된 임시 행렬입니다.
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16) // view x model
    private val modelViewProjectionMatrix = FloatArray(16) // projection x view x model
    private val viewLightDirection = FloatArray(4) // view x LIGHT_DIRECTION
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bb.root)
        displayRotationHelper = DisplayRotationHelper( /*context=*/this)

        // Set up touch listener.
        tapHelper = TapHelper( /*context=*/this)
        bb.surfaceview.setOnTouchListener(tapHelper)

        // Set up renderer.
        render = SampleRender(bb.surfaceview, this, assets)
        installRequested = false
        calculateUVTransform = true
        depthSettings.onCreate(this)
        instantPlacementSettings.onCreate(this)

        bb.settingsButton.setOnClickListener {
            val popup = PopupMenu(this@HelloArActivity, it)
            popup.setOnMenuItemClickListener { item: MenuItem -> settingsMenuClick(item) }
            popup.inflate(R.menu.settings_menu)
            popup.show()
        }
    }


    override fun onDestroy() {
        if (session != null) {
            // ARCore 세션을 명시 적으로 닫아 네이티브 리소스를 해제합니다.
            // 앱에서 close ()를 호출하기 전에 API 참조에서 중요한 고려 사항을 검토하십시오.
            // 더 복잡한 수명주기 요구 사항 :
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session!!.close()
            session = null
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        //01
        createSession()
        val session = session ?: return

        //02
        try {
            configureSession()
            session.resume()
        } catch (e: CameraNotAvailableException) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.")
            this.session = null
            return
        }
        bb.surfaceview.onResume()
        displayRotationHelper.onResume()
    }

    private fun createSession() {
        if (session != null)
            return
        var exception: Exception? = null
        var message: String? = null
        try {
            //필요한 경우 arcore를 설치
            val installStatus = ArCoreApk.getInstance().requestInstall(this, !installRequested)!!

            when (installStatus) {
                InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return
                }
                InstallStatus.INSTALLED -> Unit
            }

            // ARCore가 작동하려면 카메라 권한이 필요합니다. 아직 런타임을 얻지 못했다면
            if (!CameraPermissionHelper.hasCameraPermission(this)) {
                CameraPermissionHelper.requestCameraPermission(this)
                return
            }

            // Create the session.
            session = Session( /* context= */this)
        } catch (e: UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore"
            exception = e
        } catch (e: UnavailableUserDeclinedInstallationException) {
            message = "Please install ARCore"
            exception = e
        } catch (e: UnavailableApkTooOldException) {
            message = "Please update ARCore"
            exception = e
        } catch (e: UnavailableSdkTooOldException) {
            message = "Please update this app"
            exception = e
        } catch (e: UnavailableDeviceNotCompatibleException) {
            message = "This device does not support AR"
            exception = e
        } catch (e: Exception) {
            message = "Failed to create AR session"
            exception = e
        }
        if (message != null) {
            messageSnackbarHelper.showError(this, message)
            Log.e(TAG, "Exception creating session", exception)
        }
    }

    public override fun onPause() {
        super.onPause()
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause()
            bb.surfaceview.onPause()
            session!!.pause()
        }
    }


    override fun onSurfaceCreated(render: SampleRender) {
        // 렌더링 개체를 준비합니다. 여기에는 셰이더 및 3D 모델 파일 읽기가 포함되므로 IOException이 발생할 수 있습니다.
        try {
            depthTexture = Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE)
            planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render, depthTexture)

            // Point cloud
            pointCloudShader = Shader.createFromAssets(
                render,
                POINT_CLOUD_VERTEX_SHADER_NAME,
                POINT_CLOUD_FRAGMENT_SHADER_NAME,  /*defines=*/
                null
            ).set4("u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f))
                .set1("u_PointSize", 5.0f)
            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer = VertexBuffer(render,  /*numberOfEntriesPerVertex=*/4,  /*entries=*/null)
            val pointCloudVertexBuffers = arrayOf(
                pointCloudVertexBuffer!!
            )
            pointCloudMesh = Mesh(render, Mesh.PrimitiveMode.POINTS,  /*indexBuffer=*/null, pointCloudVertexBuffers)

            // Virtual object to render (Andy the android)
            val virtualObjectTexture = Texture.createFromAsset(render, "models/andy.png", Texture.WrapMode.CLAMP_TO_EDGE)
            virtualObjectMesh = Mesh.createFromAsset(render, "models/andy.obj")
            virtualObjectShader = createVirtualObjectShader(render, virtualObjectTexture,  /*use_depth_for_occlusion=*/false)
            virtualObjectDepthShader = createVirtualObjectShader(render, virtualObjectTexture,  /*use_depth_for_occlusion=*/                true).setTexture("u_DepthTexture", depthTexture)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        if (session == null) {
            return
        }
        if (!hasSetTextureNames) {
            session!!.setCameraTextureNames(intArrayOf(backgroundRenderer!!.textureId))
            hasSetTextureNames = true
        }

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session)
        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            val frame = session!!.update()
            val camera = frame.camera
            if (frame.hasDisplayGeometryChanged() || calculateUVTransform) {
                // The UV Transform represents the transformation between screenspace in normalized units
                // and screenspace in units of pixels.  Having the size of each pixel is necessary in the
                // virtual object shader, to perform kernel-based blur effects.
                calculateUVTransform = false
                val transform = getTextureTransformMatrix(frame)
                virtualObjectDepthShader!!.setMatrix3("u_DepthUvTransform", transform)
            }
            if (camera.trackingState == TrackingState.TRACKING
                && session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            ) {
                // The rendering abstraction leaks a bit here. Populate the depth texture with the current
                // frame data.
                try {
                    frame.acquireDepthImage().use { depthImage ->
                        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture!!.textureId)
                        GLES30.glTexImage2D(
                            GLES30.GL_TEXTURE_2D,
                            0,
                            GLES30.GL_RG8,
                            depthImage.width,
                            depthImage.height,
                            0,
                            GLES30.GL_RG,
                            GLES30.GL_UNSIGNED_BYTE,
                            depthImage.planes[0].buffer
                        )
                        val aspectRatio = depthImage.width.toFloat() / depthImage.height
                            .toFloat()
                        virtualObjectDepthShader!!.set1("u_DepthAspectRatio", aspectRatio)
                    }
                } catch (e: NotYetAvailableException) {
                    // This normally means that depth data is not available yet. This is normal so we will not
                    // spam the logcat with this.
                }
            }

            // Handle one tap per frame.
            handleTap(frame, camera)

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer!!.draw(render, frame, depthSettings.depthColorVisualizationEnabled())

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.trackingState == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(
                    this, TrackingStateHelper.getTrackingFailureReasonString(camera)
                )
                return
            }

            // Get projection matrix.
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            camera.getViewMatrix(viewMatrix, 0)

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)
            frame.acquirePointCloud().use { pointCloud ->
                if (pointCloud.timestamp > lastPointCloudTimestamp) {
                    pointCloudVertexBuffer!!.set(pointCloud.points)
                    lastPointCloudTimestamp = pointCloud.timestamp
                }
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
                pointCloudShader!!.setMatrix4("u_ModelViewProjection", modelViewProjectionMatrix)
                render.draw(pointCloudMesh, pointCloudShader)
            }

            // No tracking error at this point. If we detected any plane, then hide the
            // message UI, otherwise show searchingPlane message.
            if (hasTrackingPlane()) {
                messageSnackbarHelper.hide(this)
            } else {
                messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE)
            }

            // Visualize planes.
//      planeRenderer.drawPlanes(
//          render,
//          session.getAllTrackables(Plane.class),
//          camera.getDisplayOrientedPose(),
//          projectionMatrix);

            // Visualize anchors created by touch.
            for (coloredAnchor in anchors) {
                if (coloredAnchor.anchor.trackingState != TrackingState.TRACKING) {
                    continue
                }

                // For anchors attached to Instant Placement points, update the color once the tracking
                // method becomes FULL_TRACKING.
                if (coloredAnchor.trackable is InstantPlacementPoint
                    && coloredAnchor.trackable.trackingMethod == InstantPlacementPoint.TrackingMethod.FULL_TRACKING
                ) {
                    coloredAnchor.color = getTrackableColor(coloredAnchor.trackable)
                }

                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                coloredAnchor.anchor.pose.toMatrix(modelMatrix, 0)

                // Calculate model/view/projection matrices and view-space light direction
                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
                Matrix.multiplyMM(
                    modelViewProjectionMatrix,
                    0,
                    projectionMatrix,
                    0,
                    modelViewMatrix,
                    0
                )
                Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, LIGHT_DIRECTION, 0)

                // Update shader properties and draw
                val shader = if (depthSettings.useDepthForOcclusion()) virtualObjectDepthShader else virtualObjectShader
                shader!!.setMatrix4("u_ModelView", modelViewMatrix)
                    .setMatrix4("u_ModelViewProjection", modelViewProjectionMatrix)
                    .set4("u_ColorCorrection", colorCorrectionRgba)
                    .set4("u_ViewLightDirection", viewLightDirection)
                    .set3("u_AlbedoColor", coloredAnchor.color)
                render.draw(virtualObjectMesh, shader)
            }
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private fun handleTap(frame: Frame, camera: Camera) {
        val tap = tapHelper!!.poll()
        if (tap != null && camera.trackingState == TrackingState.TRACKING) {
            val hitResultList: List<HitResult>
            hitResultList = if (instantPlacementSettings.isInstantPlacementEnabled) {
                frame.hitTestInstantPlacement(tap.x, tap.y, APPROXIMATE_DISTANCE_METERS)
            } else {
                frame.hitTest(tap)
            }
            for (hit in hitResultList) {
                // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                val trackable = hit.trackable
                // If a plane was hit, check that it was hit inside the plane polygon.
                if ((trackable is Plane
                        && trackable.isPoseInPolygon(hit.hitPose)
                        && PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0)
                    || (trackable is Point
                        && trackable.orientationMode
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                    || trackable is InstantPlacementPoint
                ) {
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size >= 20) {
                        anchors[0].anchor.detach()
                        anchors.removeAt(0)
                    }
                    val objColor = getTrackableColor(trackable)

                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(ColoredAnchor(hit.createAnchor(), objColor, trackable))
                    // For devices that support the Depth API, shows a dialog to suggest enabling
                    // depth-based occlusion. This dialog needs to be spawned on the UI thread.
                    runOnUiThread { showOcclusionDialogIfNeeded() }

                    // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
                    // Instant Placement Point.
                    break
                }
            }
        }
    }

    /**
     * Shows a pop-up dialog on the first call, determining whether the user wants to enable
     * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
     */
    private fun showOcclusionDialogIfNeeded() {
        val isDepthSupported = session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
            return  // Don't need to show dialog.
        }

        // Asks the user whether they want to use depth-based occlusion.
        AlertDialog.Builder(this)
            .setTitle(R.string.options_title_with_depth)
            .setMessage(R.string.depth_use_explanation)
            .setPositiveButton(
                R.string.button_text_enable_depth
            ) { dialog: DialogInterface?, which: Int -> depthSettings.setUseDepthForOcclusion(true) }
            .setNegativeButton(
                R.string.button_text_disable_depth
            ) { dialog: DialogInterface?, which: Int -> depthSettings.setUseDepthForOcclusion(false) }
            .show()
    }


    private fun applySettingsMenuDialogCheckboxes() {
        depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0])
        depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1])
        instantPlacementSettings.isInstantPlacementEnabled = instantPlacementSettingsMenuDialogCheckboxes[0]
        configureSession()
    }

    private fun resetSettingsMenuDialogCheckboxes() {
        depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion()
        depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled()
        instantPlacementSettingsMenuDialogCheckboxes[0] = instantPlacementSettings.isInstantPlacementEnabled
    }

    /**
     * Checks if we detected at least one plane.
     */
    private fun hasTrackingPlane(): Boolean {
        for (plane in session!!.getAllTrackables(Plane::class.java)) {
            if (plane.trackingState == TrackingState.TRACKING) {
                return true
            }
        }
        return false
    }

    /** 세션 설정 */
    private fun configureSession() {
        val session = session ?: return
        val config = session.config

        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.depthMode = Config.DepthMode.AUTOMATIC
        } else {
            config.depthMode = Config.DepthMode.DISABLED
        }
        if (instantPlacementSettings.isInstantPlacementEnabled) {
            config.instantPlacementMode = InstantPlacementMode.LOCAL_Y_UP
        } else {
            config.instantPlacementMode = InstantPlacementMode.DISABLED
        }
        session.configure(config)
    }

    /**
     * Assign a color to the object for rendering based on the trackable type this anchor attached to.
     * For AR_TRACKABLE_POINT, it's blue color.
     * For AR_TRACKABLE_PLANE, it's green color.
     * For AR_TRACKABLE_INSTANT_PLACEMENT_POINT while tracking method is
     * SCREENSPACE_WITH_APPROXIMATE_DISTANCE, it's white color.
     * For AR_TRACKABLE_INSTANT_PLACEMENT_POINT once tracking method becomes FULL_TRACKING, it's
     * orange color.
     * The color will update for an InstantPlacementPoint once it updates its tracking method from
     * SCREENSPACE_WITH_APPROXIMATE_DISTANCE to FULL_TRACKING.
     */
    private fun getTrackableColor(trackable: Trackable): FloatArray {
        if (trackable is Point) {
            return floatArrayOf(66.0f / 255.0f, 133.0f / 255.0f, 244.0f / 255.0f)
        }
        if (trackable is Plane) {
            return floatArrayOf(139.0f / 255.0f, 195.0f / 255.0f, 74.0f / 255.0f)
        }
        if (trackable is InstantPlacementPoint) {
            if (trackable.trackingMethod
                == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE
            ) {
                return floatArrayOf(255.0f / 255.0f, 255.0f / 255.0f, 255.0f / 255.0f)
            }
            if (trackable.trackingMethod == InstantPlacementPoint.TrackingMethod.FULL_TRACKING) {
                return floatArrayOf(255.0f / 255.0f, 167.0f / 255.0f, 38.0f / 255.0f)
            }
        }
        // Fallback color.
        return floatArrayOf(0f, 0f, 0f)
    }

    companion object {
        private val TAG = HelloArActivity::class.java.simpleName
        private const val SEARCHING_PLANE_MESSAGE = "Searching for surfaces..."

        // Assumed distance from the device camera to the surface on which user will try to place objects.
        // This value affects the apparent scale of objects while the tracking method of the
        // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
        // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
        // values for AR experiences where users are expected to place objects on surfaces close to the
        // camera. Use larger values for experiences where the user will likely be standing and trying to
        // place an object on the ground or floor in front of them.
        private const val APPROXIMATE_DISTANCE_METERS = 2.0f

        // Point Cloud
        private const val POINT_CLOUD_VERTEX_SHADER_NAME = "shaders/point_cloud.vert"
        private const val POINT_CLOUD_FRAGMENT_SHADER_NAME = "shaders/point_cloud.frag"

        // Virtual object
        private const val AMBIENT_INTENSITY_VERTEX_SHADER_NAME = "shaders/ambient_intensity.vert"
        private const val AMBIENT_INTENSITY_FRAGMENT_SHADER_NAME = "shaders/ambient_intensity.frag"

        // Note: the last component must be zero to avoid applying the translational part of the matrix.
        private val LIGHT_DIRECTION = floatArrayOf(0.250f, 0.866f, 0.433f, 0.0f)

        /**
         * Returns a transformation matrix that when applied to screen space uvs makes them match
         * correctly with the quad texture coords used to render the camera feed. It takes into account
         * device orientation.
         */
        private fun getTextureTransformMatrix(frame: Frame): FloatArray {
            val frameTransform = FloatArray(6)
            val uvTransform = FloatArray(9)
            // XY pairs of coordinates in NDC space that constitute the origin and points along the two
            // principal axes.
            val ndcBasis = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)

            // Temporarily store the transformed points into outputTransform.
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcBasis,
                Coordinates2d.TEXTURE_NORMALIZED,
                frameTransform
            )

            // Convert the transformed points into an affine transform and transpose it.
            val ndcOriginX = frameTransform[0]
            val ndcOriginY = frameTransform[1]
            uvTransform[0] = frameTransform[2] - ndcOriginX
            uvTransform[1] = frameTransform[3] - ndcOriginY
            uvTransform[2] = 0F
            uvTransform[3] = frameTransform[4] - ndcOriginX
            uvTransform[4] = frameTransform[5] - ndcOriginY
            uvTransform[5] = 0F
            uvTransform[6] = ndcOriginX
            uvTransform[7] = ndcOriginY
            uvTransform[8] = 1F
            return uvTransform
        }

        @Throws(IOException::class)
        private fun createVirtualObjectShader(
            render: SampleRender, virtualObjectTexture: Texture, useDepthForOcclusion: Boolean
        ): Shader {
            return Shader.createFromAssets(
                render,
                AMBIENT_INTENSITY_VERTEX_SHADER_NAME,
                AMBIENT_INTENSITY_FRAGMENT_SHADER_NAME,
                object : HashMap<String?, String?>() {
                    init {
                        put("USE_DEPTH_FOR_OCCLUSION", if (useDepthForOcclusion) "1" else "0")
                    }
                })
                .setBlend(Shader.BlendFactor.SRC_ALPHA, Shader.BlendFactor.ONE_MINUS_SRC_ALPHA)
                .setTexture("u_AlbedoTexture", virtualObjectTexture)
                .set1("u_UpperDiffuseIntensity", 1.0f)
                .set1("u_LowerDiffuseIntensity", 0.5f)
                .set1("u_SpecularIntensity", 0.2f)
                .set1("u_SpecularPower", 8.0f)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    /** 기능별 설정을 시작하는 메뉴 버튼. */
    private fun settingsMenuClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.depth_settings) {
            launchDepthSettingsMenuDialog()
            return true
        } else if (item.itemId == R.id.instant_placement_settings) {
            launchInstantPlacementSettingsMenuDialog()
            return true
        }
        return false
    }

    /** 깊이 기반 효과를 쉽게 전환 할 수 있도록 사용자에게 확인란을 표시합니다. */
    private fun launchDepthSettingsMenuDialog() {
        // Retrieves the current settings to show in the checkboxes.
        resetSettingsMenuDialogCheckboxes()

        // Shows the dialog to the user.
        if (session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            // With depth support, the user can select visualization options.
            AlertDialog.Builder(this)
                .setTitle(R.string.options_title_with_depth)
                .setMultiChoiceItems(R.array.depth_options_array, depthSettingsMenuDialogCheckboxes) { _, which, isChecked ->
                    depthSettingsMenuDialogCheckboxes[which] = isChecked
                }
                .setPositiveButton(R.string.done) { _, _ -> applySettingsMenuDialogCheckboxes() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> resetSettingsMenuDialogCheckboxes() }
                .show()
        } else {
            // Without depth support, no settings are available.
            AlertDialog.Builder(this)
                .setTitle(R.string.options_title_without_depth)
                .setPositiveButton(
                    R.string.done
                ) { _, _ -> applySettingsMenuDialogCheckboxes() }
                .show()
        }
    }

    private fun launchInstantPlacementSettingsMenuDialog() {
        resetSettingsMenuDialogCheckboxes()
        val resources = resources
        AlertDialog.Builder(this)
            .setTitle(R.string.options_title_instant_placement)
            .setMultiChoiceItems(R.array.instant_placement_options_array, instantPlacementSettingsMenuDialogCheckboxes) { _, which, isChecked ->
                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked
            }
            .setPositiveButton(R.string.done) { _, _ -> applySettingsMenuDialogCheckboxes() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> resetSettingsMenuDialogCheckboxes() }
            .show()
    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            toast("Camera permission is needed to run this application")
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

}