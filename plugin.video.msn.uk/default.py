import xbmcplugin
import xbmcgui
import xbmc
import urllib, urllib2
import sys
import os
import re

sys.path.insert(0, os.path.join(os.getcwd(), 'lib'))
from BeautifulSoup import BeautifulSoup
from BeautifulSoup import BeautifulStoneSoup

BASE_URL = 'http://video.uk.msn.com'
ENTRY_POINT = '/browse/tv-shows/genres?rt=ajax&tagquery=%3CtagQuery%3E%3Ctags%3E%3Ctag+namespace%3D%22tvgenre%22%3E%3C%2Ftag%3E%3Ctag+namespace%3D%22videotype%22%3Etv%3C%2Ftag%3E%3C%2Ftags%3E%3Csource%3EMsn%3C%2Fsource%3E%3CdataCatalog%3EVideo%3C%2FdataCatalog%3E%3C%2FtagQuery%3E&id=ux1_4'

def get_category_list(url):
    page = urllib.urlopen(url)
    soup = BeautifulStoneSoup(page, convertEntities="html")
    categories = list()
    for item in soup.findAll('div','row'):
        print "item " + str(item)
        if item.find('a'):
            link = '/browse/tv-shows/genres?rt=ajax&tagquery=%3CtagQuery%3E%3Ctags%3E%3Ctag+namespace%3D%22tvgenre%22%3E' + str(item.a['data-tag']) + '%3C%2Ftag%3E%3Ctag+namespace%3D%22videotype%22%3Etv%3C%2Ftag%3E%3C%2Ftags%3E%3Csource%3EMsn%3C%2Fsource%3E%3CdataCatalog%3EVideo%3C%2FdataCatalog%3E%3C%2FtagQuery%3E&id=ux1_4'
            title = str(item.a['data-tag'])
            categories.append({'link' : link, 'title' : title})
    return categories    

def get_show_list(url):
    page = urllib.urlopen(url)
    soup = BeautifulStoneSoup(page, convertEntities="html")
    columns = soup.findAll('div','column')
    show_links = list()
    for column in columns:
        show_links.extend(column.findAll('a'))
    shows = list()
    for show_link in show_links:
        print show_link
        link = str(show_link['href'])
        title = str(show_link.contents[0])
        shows.append({'link' : link, 'title' : title})
    return shows

def get_episode_list(url):
    index = 0
    leave_loop = False
    episodes = list()
    while leave_loop == False:            
        index = index + 1
        page = urllib.urlopen(url + "&currentPage=" + str(index))
        soup = BeautifulSoup(page, convertEntities="html")
        result = soup.findAll('div', attrs={'class':re.compile('.*vxp_info.*')})
        if not result:
            leave_loop = True
        else:
            for item in result:
                if item.find('a'):
                    link = str(item.a['href'])
                    title = str(item.a.h5.contents[0]).strip()
                    episodes.append({'link' : link, 'title' : title})
    return episodes   

def get_episode_url(url):
    page = urllib.urlopen(url).read()
    result = re.compile('(rtmp.+?\.flv)').findall(page)
    if not result:
        result = re.compile('formatCode: 1003, url: \'(http.+?\.flv)').findall(page)
    url = result[0]
    url = unescape_html(url)
    url = unescape_slashx(url)
    return unescape_slashx(url)

def unescape_html(text):
    def fixup(m):
        text = m.group(0)
        if text[:2] == "&#":
            try:
                if text[:3] == "&#x":
                    return unichr(int(text[3:-1], 16))
                else:
                    return unichr(int(text[2:-1]))
            except ValueError:
                pass
        return text
    return re.sub("&#?\w+;", fixup, text)

def unescape_slashx(text):
    def fixup(m):
        text = m.group(0)
        if text[:2] == "\\x":
            try:
                return unichr(int(text[2:], 16))
            except ValueError:
                pass
        return text
    return re.sub("(\\\\x..)", fixup, text)
                           
def INDEX(): 
    for category in get_category_list(BASE_URL + ENTRY_POINT):
        #addLink(category['title'], sys.argv[0] + "?url=" + urllib.quote_plus(BASE_URL) + urllib.quote_plus(category['link']) + "&mode=1&name=" + urllib.quote_plus(category['title']),'')
        addDir(category['title'], BASE_URL + category['link'],1,'')

def SHOWS(url):
    print url
    for show in get_show_list(url):
        addDir(show['title'], show['link'],3,'')
        #addLink(episode['title'], sys.argv[0] + "?url=" + urllib.quote_plus(BASE_URL) + urllib.quote_plus(episode['link']) + "&mode=2&name=" + urllib.quote_plus(episode['title']),'', episode['date'])
        #addDir(episode['title'], BASE_URL + episode['link'], 1, '')    

def EPISODES(url):
    print url
    for episode in get_episode_list(url):
        #addDir(show['title'], BASE_URL + show['link'],2,'')
        addLink(episode['title'], sys.argv[0] + "?url=" + urllib.quote_plus(episode['link']) + "&mode=2&name=" + urllib.quote_plus(episode['title']),'','')
        #addDir(episode['title'], BASE_URL + episode['link'], 1, '')    

        
def VIDEO(url, name):
        print url
        url  = get_episode_url(url)
        print "playurl " + url
        item = xbmcgui.ListItem(name)
        xbmc.Player(xbmc.PLAYER_CORE_DVDPLAYER).play(url, item)
        xbmc.executebuiltin('XBMC.ActivateWindow(fullscreenvideo)')
        
def get_params():
        param=[]
        paramstring=sys.argv[2]
        if len(paramstring)>=2:
                params=sys.argv[2]
                cleanedparams=params.replace('?','')
                if (params[len(params)-1]=='/'):
                        params=params[0:len(params)-2]
                pairsofparams=cleanedparams.split('&')
                param={}
                for i in range(len(pairsofparams)):
                        splitparams={}
                        splitparams=pairsofparams[i].split('=')
                        if (len(splitparams))==2:
                                param[splitparams[0]]=splitparams[1]
                                
        return param

      
def addLink(name,url,iconimage, date):
        ok=True
        liz=xbmcgui.ListItem(name, iconImage="DefaultVideo.png", thumbnailImage=iconimage)
        liz.setInfo( type="Video", infoLabels={ "Title": name, "Date": date } )
        #sys.argv[1] = '10'
        #print "handle " + sys.argv[1]
        ok=xbmcplugin.addDirectoryItem(handle=int(sys.argv[1]),url=url,listitem=liz)
        return ok


def addDir(name,url,mode,iconimage):
        u=sys.argv[0]+"?url="+urllib.quote_plus(url)+"&mode="+str(mode)+"&name="+urllib.quote_plus(name)
        ok=True
        liz=xbmcgui.ListItem(name, iconImage="DefaultFolder.png", thumbnailImage=iconimage)
        liz.setInfo( type="Video", infoLabels={ "Title": name } )
        ok=xbmcplugin.addDirectoryItem(handle=int(sys.argv[1]),url=u,listitem=liz,isFolder=True)
        return ok
        


params=get_params()
url=None
name=None
mode=None
try:
        url=urllib.unquote_plus(params["url"])
except:
        pass
try:
        name=urllib.unquote_plus(params["name"])
except:
        pass
try:
        mode=int(params["mode"])
except:
        pass
print "Mode: "+str(mode)
print "URL: "+str(url)
print "Name: "+str(name)
print "Handle: "+ sys.argv[1]

if mode==None or url==None or len(url)<1:
        print "categories"
        INDEX()
elif mode==1:
        print "index of : "+url
        xbmcplugin.addSortMethod(int(sys.argv[1]), xbmcplugin.SORT_METHOD_DATE)#sort by date
        xbmcplugin.addSortMethod(int(sys.argv[1]), xbmcplugin.SORT_METHOD_VIDEO_TITLE)#sort by episode
        SHOWS(url)
elif mode==2:
        print "index of : "+url
        VIDEO(url, name)
elif mode==3:
        print "index of : "+url
        EPISODES(url)        

        
xbmcplugin.endOfDirectory(int(sys.argv[1]))
