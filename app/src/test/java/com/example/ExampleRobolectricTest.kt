package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SpaceScan AI", appName)
  }

  @Test
  fun `test fresh project isolation`() {
    val building = com.example.data.ProjectRepository.createFreshBuilding("test_id", "테스트 건물", 3)
    assertEquals(3, building.floors.size)
    assertEquals(0, building.overallCoveragePercent)
    for (floor in building.floors) {
      assertEquals(0, floor.rooms.size)
      assertEquals(0, floor.coveragePercent)
    }
  }
}
