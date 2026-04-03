package com.aihos.ui.render.lattice
import com.aihos.ui.render.core.AIMetricsSnapshot
import kotlin.math.sqrt
class LatticePhysics {
    private val force = FloatArray(3)
    private val diff = FloatArray(3)
    private val baseK = floatArrayOf(8f, 4f, 2f)
    fun update(l: NeuralLattice, m: AIMetricsSnapshot, dt: Float, time: Float) {
        val conf = m.confidence; val unc = 1f-conf; val gd = 0.94f+conf*0.05f
        for (i in l.nodes.indices) {
            val n = l.nodes[i]; if (!n.active) continue
            if (n.layer==0 && i==0) { n.position[0]=kotlin.math.sin(time*0.3f)*0.02f; n.position[1]=kotlin.math.sin(time*0.2f+1f)*0.02f; n.position[2]=kotlin.math.cos(time*0.25f)*0.02f; continue }
            force[0]=0f; force[1]=0f; force[2]=0f
            val k = baseK[n.layer.coerceIn(0,2)]*(0.5f+conf*0.8f)
            force[0]+=-k*(n.position[0]-n.targetPos[0]); force[1]+=-k*(n.position[1]-n.targetPos[1]); force[2]+=-k*(n.position[2]-n.targetPos[2])
            for (j in l.nodes.indices) { if (i==j) continue; val o=l.nodes[j]; if (!o.active) continue
                diff[0]=n.position[0]-o.position[0]; diff[1]=n.position[1]-o.position[1]; diff[2]=n.position[2]-o.position[2]
                val dSq=diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]; if (dSq>2.25f||dSq<0.0001f) continue
                val d=sqrt(dSq); val rF=0.05f/dSq; val inv=1f/d
                force[0]+=diff[0]*inv*rF; force[1]+=diff[1]*inv*rF; force[2]+=diff[2]*inv*rF }
            if (unc>0.1f) { val a=unc*0.3f; val t=time+n.phase*3f
                force[0]+=kotlin.math.sin(t*1.3f+i.toFloat())*a; force[1]+=kotlin.math.sin(t*0.9f+i*2f)*a*0.5f; force[2]+=kotlin.math.cos(t*1.1f+i*0.7f)*a }
            force[0]-=n.velocity[0]*2f*gd; force[1]-=n.velocity[1]*2f*gd; force[2]-=n.velocity[2]*2f*gd
            n.velocity[0]+=force[0]*dt; n.velocity[1]+=force[1]*dt; n.velocity[2]+=force[2]*dt
            n.position[0]+=n.velocity[0]*dt; n.position[1]+=n.velocity[1]*dt; n.position[2]+=n.velocity[2]*dt
            n.velocity[0]*=gd; n.velocity[1]*=gd; n.velocity[2]*=gd }
        for (c in l.connections) { if (!c.active) continue
            val fi=c.fromIndex; val ti=c.toIndex; if (fi<0||ti<0||fi>=l.nodes.size||ti>=l.nodes.size) continue
            val a=l.nodes[fi]; val b=l.nodes[ti]; if (!a.active||!b.active) continue
            diff[0]=b.position[0]-a.position[0]; diff[1]=b.position[1]-a.position[1]; diff[2]=b.position[2]-a.position[2]
            val d=sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]); if (d<0.001f) continue
            val rl=if(a.layer==0||b.layer==0) 1.2f else if(a.layer==1&&b.layer==1) 1f else 1.8f
            val f=0.3f*c.strength*(d-rl)/d
            if (fi!=0) { a.velocity[0]+=diff[0]*f*dt; a.velocity[1]+=diff[1]*f*dt; a.velocity[2]+=diff[2]*f*dt }
            if (ti!=0) { b.velocity[0]-=diff[0]*f*dt; b.velocity[1]-=diff[1]*f*dt; b.velocity[2]-=diff[2]*f*dt }
            c.strength=if(d in rl*0.5f..rl*1.5f) (c.strength+0.01f*dt).coerceAtMost(1f) else (c.strength-0.02f*dt).coerceAtLeast(0.1f) }
    }
}
