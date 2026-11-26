package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test

class SubscriptionConfigTest {
    @Test fun privateCspAndJsRemainDistinct(){assertEquals(2,SubscriptionConfig.validate("""{"spider":"https://example.invalid/module.jar","sites":[{"key":"a","name":"A","api":"csp_A","type":3},{"key":"b","name":"B","api":"https://example.invalid/rule.js","type":3}]}"""))}
    @Test fun malformedImportCannotBeAccepted(){for(json in listOf("{}","{\"sites\":[]}","{\"sites\":[{}]}","""{"sites":[{"key":"a","name":"A","api":"csp_A"},{"key":"a","name":"B","api":"csp_B"}]}""")){assertThrows(Exception::class.java){SubscriptionConfig.validate(json)}}}
}
