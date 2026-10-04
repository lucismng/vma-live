package com.example

import com.example.ui.viewmodel.TvViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun groupPriority_ordering_isCorrect() {
    val vtvPriority = TvViewModel.getGroupPriority("Kênh VTV")
    val domesticPriority = TvViewModel.getGroupPriority("Kênh Thiết yếu")
    val vtcPriority = TvViewModel.getGroupPriority("VTC")
    val htvPriority = TvViewModel.getGroupPriority("Đài TH HTV")
    val foreignPriority = TvViewModel.getGroupPriority("Kênh Quốc Tế")
    val eventPriority = TvViewModel.getGroupPriority("Sự kiện trực tiếp")

    // VTV must be first (smallest priority number)
    assertEquals(10, vtvPriority)

    // Domestic groups next
    assertEquals(20, domesticPriority)
    assertEquals(20, vtcPriority)
    assertEquals(20, htvPriority)

    // Foreign groups next
    assertEquals(40, foreignPriority)

    // Events must be last
    assertEquals(50, eventPriority)

    assertTrue(vtvPriority < domesticPriority)
    assertTrue(domesticPriority < foreignPriority)
    assertTrue(foreignPriority < eventPriority)
  }
}
