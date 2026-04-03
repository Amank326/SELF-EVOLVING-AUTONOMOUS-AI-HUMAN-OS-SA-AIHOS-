package com.aihos.ui.render.lattice
import com.aihos.ui.render.core.AIMetricsSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
class TopologyEvolver {
    companion object {
        private const val COOLDOWN = 0.5f
        private const val SPAWN_TH = 0.6f
        private const val PRUNE_TH = 0.2f
        private const val DENSE_TH = 0.7f
        private const val SPARSE_TH = 0.2f
        private const val MAX_OUTER = 24
    }
    private var cd = 0f
    private var rng = Random(42)
    private val fadingIn = IntArray(8)
    private val fadingOut = IntArray(8)
    private var fiCount = 0
    private var foCount = 0
    fun update(l: NeuralLattice, m: AIMetricsSnapshot, dt: Float, time: Float) {
        for (n in l.nodes) { if (n.active) n.age += dt }
        for (c in l.connections) { if (c.active) c.age += dt }
        updateFades(l, dt)
        updateEnergy(l, m, time)
        updateColors(l, m)
        cd -= dt
        if (cd > 0f) return
        val dc = (m.cognitiveLoad*0.4f+m.autonomyLevel*0.3f+m.evolutionRate*0.3f).coerceIn(0f,1f)
        when {
            m.evolutionRate > SPAWN_TH && countLayer(l,2) < MAX_OUTER -> { spawnOuter(l,time); cd=COOLDOWN }
            m.evolutionRate < PRUNE_TH && countLayer(l,2) > NeuralLattice.INITIAL_OUTER_NODES -> { pruneWeakest(l); cd=COOLDOWN }
            m.memoryLoad > DENSE_TH && l.activeConnCount < NeuralLattice.MAX_CONNECTIONS-4 -> { densify(l,time); cd=COOLDOWN*0.7f }
            m.memoryLoad < SPARSE_TH && l.activeConnCount > 18 -> { sparsify(l); cd=COOLDOWN*0.7f }
            dc > 0.7f && countLayer(l,1) < 10 -> { spawnInner(l,time); cd=COOLDOWN*1.5f }
        }
    }
    private fun spawnOuter(l: NeuralLattice, t: Float) {
        val idx = l.findFreeNode(); if (idx<0) return
        val a = rng.nextFloat()*(2*PI).toFloat(); val r = 2f+rng.nextFloat()*0.6f
        l.nodes[idx].activate(cos(a)*r, (rng.nextFloat()-0.5f)*0.5f, sin(a)*r, 2, a)
        l.nodes[idx].radius = 0.001f
        val ni = findNearest(l, l.nodes[idx].position[0], l.nodes[idx].position[1], l.nodes[idx].position[2], 1)
        if (ni >= 0) { val ci = l.findFreeConnection(); if (ci>=0) { l.connections[ci].activate(ni,idx,0.5f,t); l.nodes[ni].connectionCount++; l.nodes[idx].connectionCount++ } }
        if (fiCount < fadingIn.size) fadingIn[fiCount++] = idx
    }
    private fun spawnInner(l: NeuralLattice, t: Float) {
        val idx = l.findFreeNode(); if (idx<0) return
        val a = rng.nextFloat()*(2*PI).toFloat()
        l.nodes[idx].activate(cos(a)*1.4f, (rng.nextFloat()-0.5f)*0.3f, sin(a)*1.4f, 1, a)
        l.nodes[idx].radius = 0.001f
        val ci = l.findFreeConnection(); if (ci>=0) { l.connections[ci].activate(0,idx,0.8f,t); l.nodes[0].connectionCount++; l.nodes[idx].connectionCount++ }
        if (fiCount < fadingIn.size) fadingIn[fiCount++] = idx
    }
    private fun pruneWeakest(l: NeuralLattice) {
        var wi = -1; var ws = Float.MAX_VALUE
        for (i in l.nodes.indices) { val n = l.nodes[i]; if (!n.active||n.layer!=2) continue; val s=n.energy+n.connectionCount*0.3f; if (s<ws) { ws=s; wi=i } }
        if (wi<0) return
        if (foCount < fadingOut.size) fadingOut[foCount++] = wi
    }
    private fun densify(l: NeuralLattice, t: Float) {
        var bd = Float.MAX_VALUE; var bi=-1; var bj=-1
        for (i in l.nodes.indices) { if (!l.nodes[i].active) continue; for (j in i+1 until l.nodes.size) { if (!l.nodes[j].active) continue; if (l.connectionExists(i,j)) continue; val d=l.nodes[i].distanceTo(l.nodes[j]); if (d<bd && d<3f) { bd=d; bi=i; bj=j } } }
        if (bi<0) return; val ci = l.findFreeConnection(); if (ci<0) return
        l.connections[ci].activate(bi,bj,0.4f,t); l.nodes[bi].connectionCount++; l.nodes[bj].connectionCount++
    }
    private fun sparsify(l: NeuralLattice) {
        var wi=-1; var ws=Float.MAX_VALUE
        for (i in l.connections.indices) { val c=l.connections[i]; if (!c.active) continue; if (c.fromIndex==0||c.toIndex==0) continue; if (c.strength<ws) { ws=c.strength; wi=i } }
        if (wi<0) return; val c=l.connections[wi]
        if (c.fromIndex in l.nodes.indices) l.nodes[c.fromIndex].connectionCount--
        if (c.toIndex in l.nodes.indices) l.nodes[c.toIndex].connectionCount--
        c.deactivate()
    }
    private fun updateFades(l: NeuralLattice, dt: Float) {
        val fs = 2f; var i=0
        while (i<fiCount) { val idx=fadingIn[i]; val n=l.nodes[idx]; val tgt=when(n.layer){0->0.12f;1->0.07f;else->0.04f}; n.radius+=tgt*fs*dt; if (n.radius>=tgt) { n.radius=tgt; fadingIn[i]=fadingIn[--fiCount]; continue }; i++ }
        i=0; while (i<foCount) { val idx=fadingOut[i]; val n=l.nodes[idx]; n.radius-=0.04f*fs*dt; if (n.radius<=0.001f) { for (c in l.connections) { if (!c.active) continue; if (c.fromIndex==idx||c.toIndex==idx) { if (c.fromIndex in l.nodes.indices) l.nodes[c.fromIndex].connectionCount--; if (c.toIndex in l.nodes.indices) l.nodes[c.toIndex].connectionCount--; c.deactivate() } }; n.deactivate(); fadingOut[i]=fadingOut[--foCount]; continue }; i++ }
    }
    private fun updateEnergy(l: NeuralLattice, m: AIMetricsSnapshot, t: Float) {
        for (n in l.nodes) { if (!n.active) continue; val be=when(n.layer){0->0.6f+m.cognitiveLoad*0.4f; 1->0.3f+m.confidence*0.4f+m.cognitiveLoad*0.2f; else->0.1f+m.memoryLoad*0.3f+m.evolutionRate*0.2f}; n.energy=(be+sin(t*(1.5f+n.phase*0.5f))*0.1f).coerceIn(0f,1f) }
    }
    private fun updateColors(l: NeuralLattice, m: AIMetricsSnapshot) {
        for (n in l.nodes) { if (!n.active) continue; val w=m.cognitiveLoad*0.6f; n.color[0]=w*0.9f; n.color[1]=0.6f+m.confidence*0.3f-w*0.2f; n.color[2]=1f-w*0.4f; n.color[3]=0.7f+n.energy*0.3f }
    }
    private fun countLayer(l: NeuralLattice, layer: Int): Int { var c=0; for (n in l.nodes) if (n.active && n.layer==layer) c++; return c }
    private fun findNearest(l: NeuralLattice, x: Float, y: Float, z: Float, layer: Int): Int {
        var bi=-1; var bd=Float.MAX_VALUE; for (i in l.nodes.indices) { val n=l.nodes[i]; if (!n.active||n.layer!=layer) continue; val dx=n.position[0]-x; val dy=n.position[1]-y; val dz=n.position[2]-z; val d=dx*dx+dy*dy+dz*dz; if (d<bd) { bd=d; bi=i } }; return bi
    }
    fun reset() { cd=0f; rng=Random(42); fiCount=0; foCount=0 }
}
