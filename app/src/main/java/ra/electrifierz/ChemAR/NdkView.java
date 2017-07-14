/*  NDKmol - Molecular Viewer on Android NDK

     (C) Copyright 2011 - 2012, biochem_fan

     This file is part of NDKmol.

     NDKmol is free software: you can redistribute it and/or modify
     it under the terms of the GNU Lesser General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU Lesser General Public License for more details.

     You should have received a copy of the GNU Lesser General Public License
     along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package ra.electrifierz.ChemAR;

import android.util.Log;

import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.rendering.ARRenderer;
import org.artoolkit.ar.base.rendering.Cube;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NdkView extends ARRenderer {
	public float objX, objY, objZ;
	public float cameraZ, slabNear, slabFar, FOV = 20, maxD; // FIXME: FOV changing is not supported on JNI side.
	public Quaternion rotationQ;
	public int width, height;

	public float seekZoom;

	// See View.hpp for these constants
	public int proteinMode = 0;
	public int hetatmMode = 2;
	public int nucleicAcidMode = 0;
	public boolean showSidechain = false;
	public boolean showUnitcell = false;
	public boolean showSolvents = false;
	public boolean doNotSmoothen = false;
	public boolean symopHetatms = true;
	public int symmetryMode = 0;
	public int colorMode = 0;
	public boolean fogEnabled = false;
	
	private static native void nativeGLInit();
	private static native void nativeGLResize(int w, int h);
	private static native void nativeGLRender(float objX, float objY, float objZ, float ax, float ay, float az, float rot,
            float cameraZ, float slabNear, float slabFar);
	private static native void nativeLoadProtein(String path);
	private static native void nativeLoadSDF(String path);
	private static native void nativeLoadCCP4(String path);
	private static native void buildScene(int proteinMode, int hetatmMode, int symmetryMode, int colorMode, boolean showSidechain, 
			boolean showUnitcell, int nucleicAcidMode, boolean showSolvents, 
			boolean doNotSmoothen, boolean symopHetatms);
	public static native float[] nativeAdjustZoom(int symmetryMode);
	public static native void nativeUpdateMap(boolean force);
	
	public NdkView() {
		resetCamera();
	} 
	
	public void resetCamera() {
		float [] parms = nativeAdjustZoom(symmetryMode);
		objX = parms[0]; objY = parms[1]; objZ = parms[2];
		cameraZ = parms[3]; slabNear = parms[4]; slabFar = parms[5];
		maxD = parms[6];
		rotationQ = new Quaternion(1, 0, 0, 0);
	}
	
	public void prepareScene() {
		buildScene(proteinMode, hetatmMode, symmetryMode, colorMode, showSidechain, showUnitcell, 
				nucleicAcidMode, showSolvents, doNotSmoothen, symopHetatms);
	}

	private int markerID = -1;

	/**
	 * Markers can be configured here.
	 */
	@Override
	public boolean configureARScene() {

		markerID = ARToolKit.getInstance().addMarker("single;Data/chemar.patt;80");
		if (markerID < 0) return false;

		return true;
	}


	/**
	 * Override the draw function from ARRenderer.
	 */
	@Override
	public void draw(GL10 gl) {

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		// Apply the ARToolKit projection matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadMatrixf(ARToolKit.getInstance().getProjectionMatrix(), 0);

		// If the marker is visible, apply its transformation, and draw a cube
		if (ARToolKit.getInstance().queryMarkerVisible(markerID)) {
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadMatrixf(ARToolKit.getInstance().queryMarkerTransformation(markerID), 0);
            gl.glScalef((1+seekZoom/10), (1+seekZoom/10), (1+seekZoom/10));

			Vector3 axis = rotationQ.getAxis();
            nativeGLRender(objX, objY, objZ, axis.x, axis.y, axis.z, rotationQ.getAngle(),
                    cameraZ, slabNear, slabFar);
		}

		if (NDKmolActivity.GLES1) {
			if (fogEnabled) gl.glDisable(GL10.GL_FOG);
		}

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {
		super.onSurfaceChanged(gl, w, h);
		width = w; height = h;
		gl.glViewport(0, 0, width, height);
		
		nativeGLResize(w, h);
//		nativeGLInit(); // TODO: Do we need this? Do we need to re-register VBOs?
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
		nativeGLInit();

		if (NDKmolActivity.GLES1) {
			gl.glDisable(GL10.GL_FOG);
		}
	}
	
	public void loadPDB(String path) {
		nativeLoadProtein(path);	
		prepareScene();
		resetCamera();
	}
	
	public void loadSDF(String path) {
		nativeLoadSDF(path);	
		prepareScene();
		resetCamera();
	}
	
	public void loadCCP4(String path) {
		nativeLoadCCP4(path);	
		prepareScene();
	}
}
