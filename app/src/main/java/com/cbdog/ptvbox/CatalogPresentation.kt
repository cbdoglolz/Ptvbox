package com.cbdog.ptvbox

object CatalogPresentation {
    fun filtered(rows:List<Work>,year:String="",sort:Int=0,includeUpcoming:Boolean=false):List<Work>{
        val visible=rows.filter{(year.isBlank()||it.year==year)&&(includeUpcoming||!it.isUpcoming())}
        if(sort==0)return visible
        // Unknown dates remain last in both directions; never invent January 1 for a known year.
        val dated=visible.filter{it.releaseDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))}
        return (if(sort==1)dated.sortedBy{it.releaseDate}else dated.sortedByDescending{it.releaseDate})+visible.filter{it !in dated}
    }
}

data class PlayerOverlay(val controls:Boolean=true,val panel:String="",val previewMs:Long?=null){
    fun back():Pair<PlayerOverlay,Boolean> = when{
        panel.isNotBlank()->copy(panel="") to false
        previewMs!=null->copy(previewMs=null) to false
        controls->copy(controls=false) to false
        else->this to true
    }
    fun open(name:String)=copy(controls=true,panel=name,previewMs=null)
}
