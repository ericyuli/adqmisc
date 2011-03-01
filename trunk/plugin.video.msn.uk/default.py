import xbmcplugin
import xbmcgui
import xbmc
import urllib, urllib2
import sys
import os
import re
import htmlentitydefs
import cgi

sys.path.insert(0, os.path.join(os.getcwd(), 'lib'))
from BeautifulSoup import BeautifulSoup
from BeautifulSoup import BeautifulStoneSoup

BASE_URL = 'http://video.uk.msn.com'
ENTRY_POINT = '/browse/tv-shows/genres?rt=ajax&tagquery=%3CtagQuery%3E%3Ctags%3E%3Ctag+namespace%3D%22tvgenre%22%3E%3C%2Ftag%3E%3Ctag+namespace%3D%22videotype%22%3Etv%3C%2Ftag%3E%3C%2Ftags%3E%3Csource%3EMsn%3C%2Fsource%3E%3CdataCatalog%3EVideo%3C%2FdataCatalog%3E%3C%2FtagQuery%3E&id=ux1_4'

def genres(url):
    page = urllib.urlopen(url)
    soup = BeautifulStoneSoup(page, convertEntities="html")
    for item in soup.findAll('div','row'):
        if item.find('a'):
            link = '/browse/tv-shows/genres?rt=ajax&tagquery=%3CtagQuery%3E%3Ctags%3E%3Ctag+namespace%3D%22tvgenre%22%3E' + str(item.a['data-tag']) + '%3C%2Ftag%3E%3Ctag+namespace%3D%22videotype%22%3Etv%3C%2Ftag%3E%3C%2Ftags%3E%3Csource%3EMsn%3C%2Fsource%3E%3CdataCatalog%3EVideo%3C%2FdataCatalog%3E%3C%2FtagQuery%3E&id=ux1_4'
            title = str(item.a['data-tag'])
            add_list_item('shows', BASE_URL + link, title, True)
    xbmcplugin.endOfDirectory(int(sys.argv[1]))
    
def shows(url):
    page = urllib.urlopen(url)
    soup = BeautifulStoneSoup(page, convertEntities="html")
    for column in soup.findAll('div', 'column'):
        for a in column.findAll('a'):
            link = str(a['href'])
            title = a.contents[0]
            add_list_item('episodes', link, title, True)
    xbmcplugin.endOfDirectory(int(sys.argv[1]))
    
def episodes(url):
    index = 1
    while True:
        page = urllib.urlopen(url + "&currentPage=" + str(index))
        soup = BeautifulSoup(page, convertEntities="html")
        lis = soup.findAll('li', 'vxp_gallery_item')
        if not lis:
            break
    
        for item in lis:
            if not item.find('a'):
                continue
            
            thumbUrl = item.find('div', 'vxp_gallery_thumb').find('img')['src']
            link = item.find('a')['href']
            
            title = ''
            tmp = item.find('em', { "class": re.compile('.*vxp_subTitle.*') } )
            if tmp:
              title += tmp.contents[0].strip()
            tmp = item.find('h5', { "class": re.compile('.*vxp_title.*') } )
            if tmp:
              if title:
                title += ' - '
              title += tmp.contents[0].strip()

            add_list_item('episode', link, title, False, thumbUrl)

        index += 1
    xbmcplugin.endOfDirectory(int(sys.argv[1]))
    
def episode(url, name):
    page = urllib.urlopen(url).read()
    result = re.compile('(rtmp.+?\.flv)').findall(page)
    if not result:
        result = re.compile('formatCode: 1003, url: \'(http.+?\.flv)').findall(page)
    url = unescape(unescape(result[0]))

    item = xbmcgui.ListItem(name)
    xbmc.Player(xbmc.PLAYER_CORE_DVDPLAYER).play(url, item)
    xbmc.executebuiltin('XBMC.ActivateWindow(fullscreenvideo)')

def unescape(text):
    def html_fixup(m):
        text = m.group(0)
        if text[:2] == "&#":
            # character reference
            try:
                if text[:3] == "&#x":
                    return unichr(int(text[3:-1], 16))
                else:
                    return unichr(int(text[2:-1]))
            except ValueError:
                pass
        else:
            # named entity
            try:
                text = unichr(htmlentitydefs.name2codepoint[text[1:-1]])
            except KeyError:
                pass
        return text # leave as is

    def slashx_fixup(m):
        text = m.group(0)
        if text[:2] == "\\x":
            try:
                return unichr(int(text[2:], 16))
            except ValueError:
                pass
        return text
        
    text = re.sub("&#?\w+;", html_fixup, text)    
    return re.sub("(\\\\x..)", slashx_fixup, text)

def read_url():
    args = cgi.parse_qs(sys.argv[2][1:])
    state = args.get('state', [None])[0]
    url = args.get('url', [None])[0]
    name = args.get('name', [None])[0]    
    return (state, url, name)

def make_url(state=None, url=None, name=None):
    base = sys.argv[0]
    d = {}
    if state: d['state'] = state
    if url: d['url'] = url
    if name: d['name'] = name
    params = urllib.urlencode(d, True)
    return base + '?' + params

def add_list_item(state, url, name = None, isFolder = False, thumbnailUrl = None):
    icon = 'DefaultVideo.png'
    if isFolder:
      icon = 'DefaultFolder.png'
      
    item = xbmcgui.ListItem(name, iconImage = icon, thumbnailImage = thumbnailUrl)
    if name:
      item.setInfo(type = "Video", infoLabels={ "Title" : name } )
    xbmcplugin.addDirectoryItem(int(sys.argv[1]), make_url(state, url, name), item, isFolder)



if __name__ == "__main__":
    (state, url, name) = read_url()

    if state == None or state == 'genres':
        genres(BASE_URL + ENTRY_POINT)
    elif state == 'shows':
        shows(url)
    elif state == 'episodes':
        episodes(url)
    elif state == 'episode':
        episode(url, name)


