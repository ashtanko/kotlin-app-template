/*
 * Copyright 2020 Alexey Shtanko
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

object TestDependencies {
    const val JUNIT = "junit:junit:${Versions.JUNIT}"
    const val MOCKITO = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.MOCKITO}"
    const val MOCKK = "io.mockk:mockk:${Versions.MOCKK}"
    const val ASSERTJ = "org.assertj:assertj-core:${Versions.ASSERTJ}"
    const val EXT = "androidx.test.ext:junit:${Versions.EXT}"
    const val CORE = "androidx.test:core:${Versions.TEST}"
    const val RUNNER = "androidx.test:runner:${Versions.TEST}"
    const val RULES = "androidx.test:rules:${Versions.TEST}"
    const val COROUTINES_TEST =
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINES}"
    const val ARCH_CORE = "androidx.arch.core:core-testing:${Versions.ARCH_CORE}"
    const val MOCK_WEB_SERVER = "com.squareup.okhttp3:mockwebserver:${Versions.MOCK_WEB_SERVER}"
}
