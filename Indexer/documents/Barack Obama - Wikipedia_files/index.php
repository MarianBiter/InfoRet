//============================================================
// ÎNCEPUT Afișare hartă OpenStreetMap pentru coordonate
//============================================================ 
// (c) 2008 by Magnus Manske
// (c) 2010 by Strainu
// Released under GPL
 
function openStreetMapToggle () {
  var c = document.getElementById ( 'geohacklink' ) ;
  if ( !c ) return ;
  var cs = document.getElementById ( 'contentSub' ) ;
  var osm = document.getElementById ( 'openstreetmap' ) ;
 
  if ( cs && osm ) {
    if ( osm.style.display == 'none' ) {
      osm.style.display = 'block' ;
    } else {
      osm.style.display = 'none' ;
//      cs.removeChild ( osm ) ;
    }
    return false ;
  }
 
  var found_link = false ;
  var a = c.getElementsByTagName ( 'a' ) ;
  var h;
  var i;
  for ( i = 0 ; i < a.length ; i++ ) {
    h = a[i].href ;
    if ( !h.match(/geohack/) ) continue ;
    found_link = true ;
    break ;
  }
  if ( !found_link ) return ; // No geohack link found
 
  h = h.split('params=')[1] ;
 
  var url = '//tools.wmflabs.org/wiwosm/osm-on-ol/kml-on-ol.php?lang=' + mw.config.get('wgUserLanguage') + '&params=' + h ;
  i = document.createElement ( 'iframe' ) ;
  i.id = 'openstreetmap' ;
  i.style.width = '100%' ;
  i.style.height = '350px' ;
  i.style.clear = 'both' ;
  i.src = url ;
  cs.appendChild ( i ) ;
  return false ;
}
 
function openStreetMapInit () {
  var c = document.getElementById ( 'geohacklink' ) ;
  if ( !c ) return ;
 
  var buttonImage = '//upload.wikimedia.org/wikipedia/commons/thumb/9/9a/Erioll_world.svg/18px-Erioll_world.svg.png';
  var mapbutton = document.createElement('img');
  mapbutton.src = buttonImage;
  mapbutton.onclick = openStreetMapToggle;
  mapbutton.title = 'arată locația pe o hartă interactivă';
  mapbutton.alt = '';
  mapbutton.style.padding = '0px 3px 0px 0px';
  mapbutton.style.cursor = 'pointer';
  mapbutton.className = 'noprint';
 
  c.insertBefore(mapbutton, c.firstChild);
}
 
$(openStreetMapInit);
 
//============================================================
// SFÂRŞIT Afișare hartă OpenStreetMap pentru coordonate
//============================================================