package com.aihos.ui.render.hud
import android.opengl.Matrix
import com.aihos.ui.render.camera.CameraController
import kotlin.math.abs
import kotlin.math.sqrt
/**
 * HUDRaycaster — 3D ray-panel intersection for touch interaction.
 * Zero allocation per frame.
 */
class HUDRaycaster {
    private val invVP = FloatArray(16)
    private val nearPt = FloatArray(4)
    private val farPt = FloatArray(4)
    private val rayO = FloatArray(3)
    private val rayD = FloatArray(3)
    private val tv = FloatArray(4)
    private val to = FloatArray(4)
    var hoveredPanelIndex = -1; private set
    val hitPoint = FloatArray(3)
    fun raycast(sx: Float, sy: Float, sw: Int, sh: Int, cam: CameraController, hud: HUDManager): Int {
        Matrix.multiplyMM(invVP, 0, cam.projectionMatrix, 0, cam.viewMatrix, 0)
        if (!Matrix.invertM(invVP, 0, invVP, 0)) { hoveredPanelIndex = -1; return -1 }
        val nx = 2f*sx/sw - 1f; val ny = 1f - 2f*sy/sh
        unproj(nx, ny, -1f, nearPt); unproj(nx, ny, 1f, farPt)
        rayO[0]=nearPt[0]; rayO[1]=nearPt[1]; rayO[2]=nearPt[2]
        rayD[0]=farPt[0]-nearPt[0]; rayD[1]=farPt[1]-nearPt[1]; rayD[2]=farPt[2]-nearPt[2]
        val l = sqrt(rayD[0]*rayD[0]+rayD[1]*rayD[1]+rayD[2]*rayD[2])
        if (l > 0.0001f) { rayD[0]/=l; rayD[1]/=l; rayD[2]/=l }
        var cd = Float.MAX_VALUE; var ci = -1
        for (i in hud.panels.indices) {
            val p = hud.panels[i]; if (!p.active) continue
            val t = rayPlane(rayO, rayD, p.cornerTL, p.normal)
            if (t < 0f || t > cd) continue
            val hx = rayO[0]+rayD[0]*t; val hy = rayO[1]+rayD[1]*t; val hz = rayO[2]+rayD[2]*t
            if (pointInQuad(hx,hy,hz,p)) { cd=t; ci=i; hitPoint[0]=hx; hitPoint[1]=hy; hitPoint[2]=hz }
        }
        if (hoveredPanelIndex in hud.panels.indices) hud.panels[hoveredPanelIndex].hovered = false
        hoveredPanelIndex = ci
        if (ci >= 0) hud.panels[ci].hovered = true
        return ci
    }
    fun clearHover(hud: HUDManager) {
        if (hoveredPanelIndex in hud.panels.indices) hud.panels[hoveredPanelIndex].hovered = false
        hoveredPanelIndex = -1
    }
    private fun unproj(nx: Float, ny: Float, nz: Float, out: FloatArray) {
        tv[0]=nx; tv[1]=ny; tv[2]=nz; tv[3]=1f
        Matrix.multiplyMV(to, 0, invVP, 0, tv, 0)
        if (abs(to[3]) > 0.00001f) { out[0]=to[0]/to[3]; out[1]=to[1]/to[3]; out[2]=to[2]/to[3] }
        else { out[0]=to[0]; out[1]=to[1]; out[2]=to[2] }
    }
    private fun rayPlane(o: FloatArray, d: FloatArray, pp: FloatArray, pn: FloatArray): Float {
        val den = pn[0]*d[0]+pn[1]*d[1]+pn[2]*d[2]
        if (abs(den) < 0.00001f) return -1f
        val t = ((pp[0]-o[0])*pn[0]+(pp[1]-o[1])*pn[1]+(pp[2]-o[2])*pn[2]) / den
        return if (t >= 0f) t else -1f
    }
    private fun pointInQuad(px: Float, py: Float, pz: Float, p: HUDPanel): Boolean {
        return sameS(px,py,pz,p.cornerTL,p.cornerTR,p.cornerBL) &&
               sameS(px,py,pz,p.cornerTR,p.cornerBR,p.cornerTL) &&
               sameS(px,py,pz,p.cornerBR,p.cornerBL,p.cornerTR) &&
               sameS(px,py,pz,p.cornerBL,p.cornerTL,p.cornerBR)
    }
    private fun sameS(px:Float,py:Float,pz:Float, a:FloatArray, b:FloatArray, r:FloatArray): Boolean {
        val ex=b[0]-a[0]; val ey=b[1]-a[1]; val ez=b[2]-a[2]
        val apx=px-a[0]; val apy=py-a[1]; val apz=pz-a[2]
        val arx=r[0]-a[0]; val ary=r[1]-a[1]; val arz=r[2]-a[2]
        val c1x=ey*apz-ez*apy; val c1y=ez*apx-ex*apz; val c1z=ex*apy-ey*apx
        val c2x=ey*arz-ez*ary; val c2y=ez*arx-ex*arz; val c2z=ex*ary-ey*arx
        return (c1x*c2x+c1y*c2y+c1z*c2z) >= 0f
    }
}
