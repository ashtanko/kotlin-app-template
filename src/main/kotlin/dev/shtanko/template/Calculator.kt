/*
 * Copyright 2021 Alexey Shtanko
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

import kotlin.math.ln
import kotlin.math.sqrt

class Calculator {
    fun add(a: Int, b: Int) = a + b
    fun divide(a: Int, b: Int):Double = if (b == 0) {
        throw DivideByZeroException(a)
    } else {
        a.toDouble() / b.toDouble()
    }

    fun square(a: Int) = a * a

    fun squareRoot(a: Int) = sqrt(a.toDouble())

    fun log(base: Int, value: Int) = ln(value.toDouble()) / ln(base.toDouble())
}
