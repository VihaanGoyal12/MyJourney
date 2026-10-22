package com.vihaan.myjourney

import com.vihaan.myjourney.destination.GeoapifyClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DestinationParsingTest {

    @Test
    fun geoapifyJson_parsesCorrectly() {
        val sampleGeoapifyJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": {
                "name": "Meerut",
                "country": "India",
                "country_code": "in",
                "state": "Uttar Pradesh",
                "city": "Meerut",
                "lon": 77.7059559,
                "lat": 28.984644,
                "formatted": "Meerut, Uttar Pradesh, India",
                "place_id": "51a3ddf9689e6d5340590680a6b7e6fc3c40f00101f9011e40040000000000c00208"
              },
              "geometry": {
                "type": "Point",
                "coordinates": [77.7059559, 28.984644]
              }
            }
          ]
        }
        """.trimIndent()

        val client = GeoapifyClient(apiKey = "test_key")
        // Use reflection to test private parseGeoapifyFeatures method cleanly
        val method = GeoapifyClient::class.java.getDeclaredMethod("parseGeoapifyFeatures", String::class.java)
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val results = method.invoke(client, sampleGeoapifyJson) as List<com.vihaan.myjourney.destination.Destination>

        assertEquals(1, results.size)
        val first = results[0]
        assertEquals("Meerut", first.name)
        assertEquals("Meerut, Uttar Pradesh, India", first.formattedAddress)
        assertEquals(28.984644, first.latitude, 0.0001)
        assertEquals(77.7059559, first.longitude, 0.0001)
        assertEquals("51a3ddf9689e6d5340590680a6b7e6fc3c40f00101f9011e40040000000000c00208", first.placeId)
    }

    @Test
    fun emptyOrInvalidJson_returnsEmptyListWithoutCrashing() {
        val client = GeoapifyClient(apiKey = "test_key")
        val method = GeoapifyClient::class.java.getDeclaredMethod("parseGeoapifyFeatures", String::class.java)
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val emptyResults = method.invoke(client, "{}") as List<com.vihaan.myjourney.destination.Destination>
        assertEquals(0, emptyResults.size)
    }
}
