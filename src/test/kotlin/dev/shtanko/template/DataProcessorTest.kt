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

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DataProcessorTest {

    private lateinit var dataProcessor: DataProcessor
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        dataProcessor = DataProcessor(testDispatcher)
    }

    @Test
    fun `fetchIds should return list of ids`() = runTest(testDispatcher) {
        // Act
        val result = dataProcessor.fetchIds()

        // Assert
        assertEquals(listOf(1, 2, 3, 4, 5), result)
    }

    @Test
    fun `processId should return processed string`() = runTest(testDispatcher) {
        // Act
        val result = dataProcessor.processId(1)

        // Assert
        assertEquals("Processed ID: 1", result)
    }

    @Test
    fun `processDataStream should emit results and handle errors using Turbine`() = runTest(testDispatcher) {
        // Act & Assert
        dataProcessor.processDataStream().test {
            // Assert item 1
            assertEquals(Result.success("Processed ID: 1"), awaitItem())

            // Assert item 2
            assertEquals(Result.success("Processed ID: 2"), awaitItem())

            // Item 3 will throw IllegalArgumentException in processId
            // The retry operator will re-execute the upstream flow 2 times
            // 1st retry:
            assertEquals(Result.success("Processed ID: 1"), awaitItem())
            assertEquals(Result.success("Processed ID: 2"), awaitItem())

            // 2nd retry:
            assertEquals(Result.success("Processed ID: 1"), awaitItem())
            assertEquals(Result.success("Processed ID: 2"), awaitItem())

            // After 2 retries, the exception is caught by the catch block
            val failureResult = awaitItem()
            assertTrue(failureResult.isFailure)
            assertEquals("Invalid ID: 3", failureResult.exceptionOrNull()?.message)

            // Flow should complete
            awaitComplete()
        }
    }
}
