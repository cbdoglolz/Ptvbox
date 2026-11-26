package com.cbdog.ptvbox

import org.jsoup.nodes.Element
import java.util.Locale

/**
 * Android's JAXP DOM does not expose the user-data map expected by Jsoup's
 * selectXpath helper. Kazumi rules use a narrow XPath subset, so evaluate that
 * subset directly against Jsoup's DOM instead of depending on Android XML APIs.
 */
internal object XPathLite056 {
    private data class Step(val descendant:Boolean,val css:String)

    fun select(context:Element,expression:String):List<Element> {
        val xpath=expression.trim()
        if(xpath.isEmpty()||xpath=="/"||xpath=="//")return emptyList()
        if(xpath==".")return listOf(context)
        if(xpath.startsWith("/self::"))return selfAndSiblings(context,xpath)
        val parts=xpath.split('|').map{it.trim()}.filter{it.isNotBlank()}
        if(parts.size>1)return parts.flatMap{select(context,it)}.distinct()
        var rest=xpath
        val descendantStart=rest.startsWith("//")||rest.startsWith(".//")
        rest=rest.removePrefix(".//").removePrefix("//").removePrefix("/")
        val steps=parseSteps(rest,descendantStart)
        if(steps.isEmpty())return emptyList()
        val first=steps.first()
        var rows=when {
            descendantStart->context.select(first.css)
            else->context.children().filter{it.`is`(first.css)}
        }
        for(step in steps.drop(1)){
            rows=rows.flatMap{node->
                if(step.descendant)node.select(step.css)
                else node.children().filter{it.`is`(step.css)}
            }
        }
        return rows.distinct()
    }

    fun text(context:Element,expression:String):String {
        val xpath=expression.trim()
        if(xpath=="."||xpath.endsWith("/text()")){
            val nodes=if(xpath==".")listOf(context)else select(context,xpath.removeSuffix("/text()"))
            return nodes.firstOrNull()?.text().orEmpty()
        }
        return select(context,xpath).firstOrNull()?.text().orEmpty()
    }

    private fun parseSteps(path:String,initialDescendant:Boolean):List<Step> {
        val result=mutableListOf<Step>()
        var index=0
        var descendant=initialDescendant
        while(index<path.length){
            while(index<path.length&&path[index]=='/'){descendant=true;index++}
            val start=index
            var brackets=0
            var quote:Char?=null
            while(index<path.length){
                val ch=path[index]
                if(quote!=null){
                    if(ch==quote)quote=null
                }else when(ch){
                    '\'','"'->quote=ch
                    '['->brackets++
                    ']'->if(brackets>0)brackets--
                    '/'->
                        if(brackets==0)break
                }
                index++
            }
            if(start==index)continue
            val raw=path.substring(start,index)
            result+=Step(descendant,toCss(raw))
            descendant=false
        }
        return result
    }

    private fun toCss(rawStep:String):String {
        var step=rawStep.trim()
        if(step.endsWith("/text()"))step=step.removeSuffix("/text()")
        val bracket=step.indexOf('[')
        var tag=if(bracket>=0)step.substring(0,bracket)else step
        if(tag.isBlank())tag="*"
        val predicates=if(bracket>=0)Regex("\\[([^\\]]+)]").findAll(step.substring(bracket)).map{it.groupValues[1].trim()}.toList()else emptyList()
        val css=StringBuilder(tag)
        for(predicate in predicates){
            val p=predicate.replace(" ", "")
            when {
                p.matches(Regex("\\d+"))->css.append(":nth-of-type(").append(p).append(')')
                p.matches(Regex("contains\\(@class,['\\\"][^'\\\"]+['\\\"]\\)"))->attributeContains(css,"class",betweenQuotes(p))
                p.matches(Regex("@class\\*=['\\\"][^'\\\"]+['\\\"]"))->attributeContains(css,"class",betweenQuotes(p))
                p.matches(Regex("@class=['\\\"][^'\\\"]+['\\\"]"))->attributeContains(css,"class",betweenQuotes(p))
                p.matches(Regex("@id=['\\\"][^'\\\"]+['\\\"]"))->css.append("#").append(escapeSelector(betweenQuotes(p)))
                p.matches(Regex("starts-with\\(@id,['\\\"][^'\\\"]+['\\\"]\\)"))->css.append("[id^='").append(escapeValue(betweenQuotes(p))).append("']")
                p.matches(Regex("contains\\(@href,['\\\"][^'\\\"]+['\\\"]\\)"))->css.append("[href*='").append(escapeValue(betweenQuotes(p))).append("']")
            }
        }
        return css.toString()
    }

    private fun attributeContains(css:StringBuilder,attribute:String,value:String){css.append('[').append(attribute).append("*='").append(escapeValue(value)).append("']")}
    private fun betweenQuotes(value:String):String=value.substringAfter('\'').substringBefore('\'').ifBlank{value.substringAfter('"').substringBefore('"')}
    private fun escapeValue(value:String)=value.replace("\\","\\\\").replace("'","\\'")
    private fun escapeSelector(value:String)=value.replace(Regex("([^a-zA-Z0-9_-])")){"\\${it.value}"}

    /**
     * The bundled EZDMW rule selects following circuit-switch anchors from each
     * route element. Translating that XPath union is unnecessary; the switch
     * anchors are direct siblings with the same circuit prefix.
     */
    private fun selfAndSiblings(context:Element,xpath:String):List<Element> {
        if(!xpath.contains("following-sibling::a"))return emptyList()
        return context.parent()?.children()?.filter{
            it.tagName().equals("a",true)&&it.className().lowercase(Locale.ROOT).startsWith("circuit_switch")
        }.orEmpty()
    }
}
