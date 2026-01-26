package com.aihos.ai.neural

import kotlin.math.*
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Advanced Neural Network Engine
 * Implements:
 * - Multi-layer perceptron with backpropagation
 * - Multiple activation functions (ReLU, Sigmoid, Tanh)
 * - Gradient descent optimization
 * - Deep learning with arbitrary architecture
 * - Pattern recognition and prediction
 */

data class Neuron(
    val id: String = java.util.UUID.randomUUID().toString(),
    var weights: FloatArray,
    var bias: Float = Random.nextFloat() * 2 - 1,
    var activation: String = "relu",
    var lastInput: FloatArray = FloatArray(0),
    var lastOutput: Float = 0f,
    var gradient: Float = 0f
) {
    fun forward(inputs: FloatArray): Float {
        lastInput = inputs
        val sum = inputs.indices.sumOf { (inputs[it] * weights[it]).toDouble() }.toFloat() + bias
        lastOutput = activationFunction(sum, activation)
        return lastOutput
    }

    private fun activationFunction(value: Float, function: String): Float {
        return when (function) {
            "relu" -> max(0f, value)
            "sigmoid" -> 1f / (1f + exp(-value))
            "tanh" -> tanh(value)
            "linear" -> value
            else -> value
        }
    }

    fun activationDerivative(output: Float, function: String): Float {
        return when (function) {
            "relu" -> if (output > 0) 1f else 0f
            "sigmoid" -> output * (1 - output)
            "tanh" -> 1 - (output * output)
            "linear" -> 1f
            else -> 1f
        }
    }
}

data class NeuralLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val inputSize: Int,
    val outputSize: Int,
    val neurons: MutableList<Neuron> = mutableListOf(),
    var learningRate: Float = 0.01f,
    var activation: String = "relu"
) {
    init {
        repeat(outputSize) {
            neurons.add(Neuron(weights = FloatArray(inputSize) { Random.nextFloat() * 2 - 1 }, activation = activation))
        }
    }

    fun forward(inputs: FloatArray): FloatArray {
        return FloatArray(neurons.size) { neurons[it].forward(inputs) }
    }

    fun backward(outputErrors: FloatArray): FloatArray {
        val inputErrors = FloatArray(neurons[0].weights.size)

        for ((neuronIdx, neuron) in neurons.withIndex()) {
            val error = outputErrors[neuronIdx]
            val derivative = neuron.activationDerivative(neuron.lastOutput, activation)
            neuron.gradient = error * derivative

            // Update weights
            for (w in neuron.weights.indices) {
                val weightGradient = neuron.gradient * neuron.lastInput[w]
                neuron.weights[w] -= learningRate * weightGradient
                inputErrors[w] += neuron.gradient * neuron.weights[w]
            }

            // Update bias
            neuron.bias -= learningRate * neuron.gradient
        }

        return inputErrors
    }
}

/**
 * Complete Neural Network with multiple layers
 */
class NeuralNetwork(
    private val architecture: List<Int>, // e.g., [10, 64, 32, 5] = 10 input → 64 hidden → 32 hidden → 5 output
    private val activations: List<String> = listOf()
) {
    private val layers = mutableListOf<NeuralLayer>()
    private val _trainingLoss = MutableStateFlow(1f)
    val trainingLoss: StateFlow<Float> = _trainingLoss
    
    private val _accuracy = MutableStateFlow(0f)
    val accuracy: StateFlow<Float> = _accuracy
    
    private val _predictions = MutableStateFlow<List<Float>>(emptyList())
    val predictions: StateFlow<List<Float>> = _predictions
    
    private var epoch = 0

    init {
        // Build network layers
        for (i in 0 until architecture.size - 1) {
            val inputSize = architecture[i]
            val outputSize = architecture[i + 1]
            val activation = if (i < activations.size) activations[i] else if (i == architecture.size - 2) "sigmoid" else "relu"
            layers.add(NeuralLayer(inputSize = inputSize, outputSize = outputSize, activation = activation))
        }
    }

    /**
     * Forward pass through network
     */
    fun forward(input: FloatArray): FloatArray {
        var current = input
        for (layer in layers) {
            current = layer.forward(current)
        }
        return current
    }

    /**
     * Train network with backpropagation
     */
    suspend fun train(inputs: List<FloatArray>, targets: List<FloatArray>, epochs: Int = 100) {
        repeat(epochs) { epochNum ->
            epoch = epochNum
            var totalLoss = 0f

            for (i in inputs.indices) {
                // Forward pass
                val prediction = forward(inputs[i])
                _predictions.emit(prediction.toList())

                // Calculate loss (MSE)
                var loss = 0f
                val errors = FloatArray(prediction.size)
                for (j in prediction.indices) {
                    val error = prediction[j] - targets[i][j]
                    errors[j] = error
                    loss += error * error
                }
                totalLoss += loss / prediction.size

                // Backward pass
                var layerErrors = errors
                for (j in layers.size - 1 downTo 0) {
                    layerErrors = layers[j].backward(layerErrors)
                }
            }

            val avgLoss = totalLoss / inputs.size
            _trainingLoss.emit(avgLoss)

            // Calculate accuracy every 10 epochs
            if (epochNum % 10 == 0) {
                var correct = 0
                for (i in inputs.indices) {
                    val prediction = forward(inputs[i])
                    val predictedClass = prediction.indices.maxByOrNull { prediction[it] } ?: 0
                    val targetClass = targets[i].indices.maxByOrNull { targets[i][it] } ?: 0
                    if (predictedClass == targetClass) correct++
                }
                _accuracy.emit(correct.toFloat() / inputs.size)
            }
        }
    }

    /**
     * Predict on single input
     */
    fun predict(input: FloatArray): FloatArray {
        return forward(input)
    }

    /**
     * Get network summary
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "architecture" to architecture,
            "layerCount" to layers.size,
            "totalNeurons" to layers.sumOf { it.neurons.size },
            "trainingLoss" to _trainingLoss.value,
            "accuracy" to _accuracy.value,
            "epoch" to epoch,
            "layerDetails" to layers.map { mapOf(
                "inputSize" to it.inputSize,
                "outputSize" to it.outputSize,
                "activation" to it.activation,
                "learningRate" to it.learningRate
            ) }
        )
    }

    /**
     * Set learning rate
     */
    fun setLearningRate(rate: Float) {
        for (layer in layers) {
            layer.learningRate = rate
        }
    }

    /**
     * Save weights to list
     */
    fun saveWeights(): List<Map<String, Any>> {
        return layers.map { layer ->
            mapOf(
                "layerId" to layer.id,
                "weights" to layer.neurons.map { it.weights.toList() },
                "biases" to layer.neurons.map { it.bias }
            )
        }
    }

    /**
     * Load weights from list
     */
    fun loadWeights(saved: List<Map<String, Any>>) {
        for ((layerIdx, layerData) in saved.withIndex()) {
            if (layerIdx < layers.size) {
                @Suppress("UNCHECKED_CAST")
                val weights = (layerData["weights"] as? List<List<Float>>) ?: continue
                @Suppress("UNCHECKED_CAST")
                val biases = (layerData["biases"] as? List<Float>) ?: continue

                for ((neuronIdx, neuron) in layers[layerIdx].neurons.withIndex()) {
                    if (neuronIdx < weights.size && neuronIdx < biases.size) {
                        neuron.weights = weights[neuronIdx].toFloatArray()
                        neuron.bias = biases[neuronIdx]
                    }
                }
            }
        }
    }

    /**
     * Reset network
     */
    fun reset() {
        for (layer in layers) {
            for (neuron in layer.neurons) {
                for (i in neuron.weights.indices) {
                    neuron.weights[i] = Random.nextFloat() * 2 - 1
                }
                neuron.bias = Random.nextFloat() * 2 - 1
                neuron.gradient = 0f
            }
        }
        epoch = 0
    }
}

/**
 * Ensemble of neural networks for improved predictions
 */
class NeuralNetworkEnsemble(private val networkCount: Int = 3) {
    private val networks = mutableListOf<NeuralNetwork>()
    private val _ensembleAccuracy = MutableStateFlow(0f)
    val ensembleAccuracy: StateFlow<Float> = _ensembleAccuracy

    fun addNetwork(network: NeuralNetwork) {
        networks.add(network)
    }

    /**
     * Predict with ensemble voting
     */
    fun predictEnsemble(input: FloatArray): FloatArray {
        if (networks.isEmpty()) return FloatArray(0)

        val predictions = networks.map { it.predict(input) }

        // Average predictions
        val result = FloatArray(predictions[0].size)
        for (i in result.indices) {
            result[i] = predictions.map { it[i] }.average().toFloat()
        }

        return result
    }

    /**
     * Train all networks
     */
    suspend fun trainEnsemble(inputs: List<FloatArray>, targets: List<FloatArray>, epochs: Int = 100) {
        for (network in networks) {
            network.train(inputs, targets, epochs)
        }
    }

    /**
     * Get ensemble accuracy
     */
    suspend fun evaluateEnsemble(inputs: List<FloatArray>, targets: List<FloatArray>): Float {
        var correct = 0
        for (i in inputs.indices) {
            val prediction = predictEnsemble(inputs[i])
            val predictedClass = prediction.indices.maxByOrNull { prediction[it] } ?: 0
            val targetClass = targets[i].indices.maxByOrNull { targets[i][it] } ?: 0
            if (predictedClass == targetClass) correct++
        }

        val accuracy = correct.toFloat() / inputs.size
        _ensembleAccuracy.emit(accuracy)
        return accuracy
    }
}
