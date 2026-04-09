/*
 * Copyright 2026 Oleksii Shtanko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.shtanko.template

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.withContext

/**
 * A complex coroutines example demonstrating best practices.
 * - Uses CoroutineDispatcher injection for testability.
 * - Uses Flows for streaming data.
 * - Handles exceptions correctly.
 * - Simulates a network or database operation.
 *
 * @param ioDispatcher The CoroutineDispatcher used to offload logic.
 */
class DataProcessor(
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Fetches a list of IDs.
     *
     * @return List of IDs.
     */
    suspend fun fetchIds(): List<Int> = withContext(ioDispatcher) {
        delay(100.milliseconds)  // Simulate network delay
        listOf(1, 2, 3, 4, 5)
    }

    /**
     * Processes a single ID.
     *
     * @param id The ID to process.
     * @return Processed string representation.
     */
    suspend fun processId(id: Int): String = withContext(ioDispatcher) {
        delay(50.milliseconds)  // Simulate processing time
        require(id != 3) { "Invalid ID: $id" }
        "Processed ID: $id"
    }

    /**
     * A flow that fetches IDs, processes them, and emits the results.
     * Demonstrates flow operators, retry logic, and error handling.
     *
     * @return A Flow of processed Result strings.
     */
    fun processDataStream(): Flow<Result<String>> = flow {
        val ids = fetchIds()
        for (id in ids) {
            emit(id)
        }
    }
        .map { id ->
            val processed = processId(id)
            Result.success(processed)
        }
        .retry(retries = 2) { cause ->
            // Retry on specific exceptions, e.g., network errors.
            // For this example, we'll retry on any exception just to demonstrate the operator.
            cause is Exception
        }
        .catch { e ->
            emit(Result.failure(e))
        }
        .flowOn(ioDispatcher)  // Execute upstream operations on the provided dispatcher
}
