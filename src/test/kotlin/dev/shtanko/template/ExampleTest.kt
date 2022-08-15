/*
 * Copyright 2022 Oleksii Shtanko
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

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ExampleTest {

    private val calculator = Calculator()

    @Test
    fun `When adding 1 and 3 then answer is 4`() {
        assertEquals(4, calculator.add(1, 3))
    }

    @Test
    fun `The square of a number should be equal to that number multiplied in itself`() {
        assertAll(
            Executable { assertEquals(1, calculator.square(1)) },
            Executable { assertEquals(4, calculator.square(2)) },
            Executable { assertEquals(9, calculator.square(3)) }
        )
    }

    @Test
    fun `Divide test`() {
        assertAll(
            Executable { assertEquals(2.0, calculator.divide(4, 2)) },
            Executable { assertEquals(1.0, calculator.divide(1, 1)) },
            Executable { assertEquals(1.5, calculator.divide(3, 2)) }
        )
    }

    @Test
    fun `Dividing by zero should throw the DivideByZeroException`() {
        val exception = assertThrows(DivideByZeroException::class.java) {
            calculator.divide(5, 0)
        }
        assertEquals(5, exception.numerator)
    }

    @Test
    fun `isEmpty should return true for empty lists`() {
        val list = listOf<String>()
        assertTrue(list::isEmpty)
    }

    @TestFactory
    fun `Squares test`() = listOf(
        DynamicTest.dynamicTest("when I calculate 1^2 then I get 1") {
            assertEquals(1, calculator.square(1))
        },
        DynamicTest.dynamicTest("when I calculate 2^2 then I get 4") {
            assertEquals(4, calculator.square(2))
        },
        DynamicTest.dynamicTest("when I calculate 3^2 then I get 9") {
            assertEquals(9, calculator.square(3))
        }
    )

    @TestFactory
    fun `Squares map test`() = listOf(
        1 to 1,
        2 to 4,
        3 to 9,
        4 to 16,
        5 to 25
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when I calculate $input^2 then I get $expected") {
            assertEquals(expected, calculator.square(input))
        }
    }

    @ParameterizedTest
    @CsvSource(
        "1, 1",
        "2, 4",
        "3, 9"
    )
    fun `Squares test`(input: Int, expected: Int) {
        assertEquals(expected, input * input)
    }

    @Tags(Tag("slow"), Tag("logarithms"))
    @Test
    fun `Log to base 2 of 8 should be equal to 3`() {
        assertEquals(3.0, calculator.log(2, 8))
    }
}
