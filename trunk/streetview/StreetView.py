#!/usr/bin/python

import urllib
import urllib2
import libxml2
import inspect
import sys
import zlib
import base64
import struct

# Panorama meta parameters
# Other Parameters: review=1  		# review  (?)
# 	            ph=1 		# lookaround
# 	            tr=1		# video
# 	            renderer=cubic,spherical
#                   dm=1 		# Unknown
#		    pm=1 		# Unknown
# 		    ll=<lat>,<lon> 	# lat,lon to find panorama for
#		    radius=<value> 	# search radius for lat/lon->panoid 
#                   output=xml  	# Output metadata xml
#                   v=4			# version?
#		    panoid=<value>	# panorama id


BaseUri = 'http://maps.google.com/cbk';

def FindPano(lat, lon, radius):
	uri = '%s?output=xml&v=4&ll=%s,%s&radius=%s' % (BaseUri, lat, lon, radius)
	f = urllib2.urlopen(uri)
	findpanoxml = f.read()
	f.close()
	if not findpanoxml.find('data_properties'):
		return None
	return Panorama(libxml2.parseDoc(findpanoxml))

def GetPanoMetadata(panoId):
	uri = '%s?output=xml&v=4&dm=1&pm=1&ph=1&panoid=%s' % (BaseUri, panoId)
	f = urllib2.urlopen(uri)
	findpanoxml = f.read()
	f.close()
	if not findpanoxml.find('data_properties'):
		return None
	return Panorama(libxml2.parseDoc(findpanoxml))



class Panorama:
	
	def __init__(self, panodoc):
		self.PanoDoc = panodoc
		panoDocCtx = self.PanoDoc.xpathNewContext()

		self.PanoId = panoDocCtx.xpathEval("/panorama/data_properties/@pano_id")[0].content
		self.ImageWidth = panoDocCtx.xpathEval("/panorama/data_properties/@image_width")[0].content
		self.ImageHeight = panoDocCtx.xpathEval("/panorama/data_properties/@image_height")[0].content
		self.TileWidth = panoDocCtx.xpathEval("/panorama/data_properties/@tile_width")[0].content
		self.TileHeight = panoDocCtx.xpathEval("/panorama/data_properties/@tile_height")[0].content
		self.NumZoomLevels = panoDocCtx.xpathEval("/panorama/data_properties/@num_zoom_levels")[0].content
		self.Lat = panoDocCtx.xpathEval("/panorama/data_properties/@lat")[0].content
		self.Lon = panoDocCtx.xpathEval("/panorama/data_properties/@lng")[0].content
		self.OriginalLat = panoDocCtx.xpathEval("/panorama/data_properties/@original_lat")[0].content
		self.OriginalLon = panoDocCtx.xpathEval("/panorama/data_properties/@original_lng")[0].content
		self.Copyright = panoDocCtx.xpathEval("/panorama/data_properties/copyright/text()")[0].content
		self.Text = panoDocCtx.xpathEval("/panorama/data_properties/text/text()")[0].content
		self.Region = panoDocCtx.xpathEval("/panorama/data_properties/region/text()")[0].content
		self.Country = panoDocCtx.xpathEval("/panorama/data_properties/country/text()")[0].content

		self.ProjectionType = panoDocCtx.xpathEval("/panorama/projection_properties/@projection_type")[0].content
		self.ProjectionPanoYawDeg = panoDocCtx.xpathEval("/panorama/projection_properties/@pano_yaw_deg")[0].content
		self.ProjectionTiltYawDeg = panoDocCtx.xpathEval("/panorama/projection_properties/@tilt_yaw_deg")[0].content
		self.ProjectionTiltPitchDeg = panoDocCtx.xpathEval("/panorama/projection_properties/@tilt_pitch_deg")[0].content
		
		self.AnnotationLinks = []
		for cur in panoDocCtx.xpathEval("/panorama/annotation_properties/link"):
			self.AnnotationLinks.append({ 'YawDeg': cur.xpathEval("@yaw_deg")[0].content,
					    'PanoId': cur.xpathEval("@pano_id")[0].content,
					    'RoadARGB': cur.xpathEval("@road_argb")[0].content,
					    'Text': cur.xpathEval("link_text/text()")[0].content,
			})
		
		tmp = panoDocCtx.xpathEval("/panorama/model/pano_map/text()")
		if len(tmp) > 0:
			tmp = tmp[0].content
			tmp = zlib.decompress(base64.urlsafe_b64decode(tmp + self.MakePadding(tmp)))
			self.DecodePanoMap(tmp)
		
		tmp = panoDocCtx.xpathEval("/panorama/model/depth_map/text()")
		if len(tmp) > 0:
			tmp = tmp[0].content
			tmp = zlib.decompress(base64.urlsafe_b64decode(tmp + self.MakePadding(tmp)))
			self.DecodeDepthMap(tmp)


	def MakePadding(self, s):
		return (4 - (len(s) % 4)) * '='


	def DecodePanoMap(self, raw):
		pos = 0
		
		(headerSize, numPanos, panoWidth, panoHeight, panoIndicesOffset) = struct.unpack('<BHHHB', raw[0:8])
		if headerSize != 8 or panoIndicesOffset != 8:
			print "Invalid panomap data"
			return
		pos += headerSize
		
		self.PanoMapIndices = list(raw[panoIndicesOffset:panoIndicesOffset + (panoWidth * panoHeight)])
		pos += len(self.PanoMapIndices)
		
		self.PanoMapPanos = []
		for i in xrange(0, numPanos - 1):
			self.PanoMapPanos.append({ 'panoid': raw[pos: pos+ 22]})
			pos += 22
			
		for i in xrange(0, numPanos - 1):
			(lat, lon) = struct.unpack('<ff', raw[pos:pos+8])
			self.PanoMapPanos[i]['lat'] = lat
			self.PanoMapPanos[i]['lon'] = lon
			pos+=8

	def DecodeDepthMap(self, raw):
		pos = 0

		(headerSize, numPlanes, panoWidth, panoHeight, planeIndicesOffset) = struct.unpack('<BHHHB', raw[0:8])
		if headerSize != 8 or planeIndicesOffset != 8:
			print "Invalid depthmap data"
			return
		pos += headerSize

		self.DepthMapIndices = list(raw[planeIndicesOffset:planeIndicesOffset + (panoWidth * panoHeight)])
		pos += len(self.DepthMapIndices)

		self.DepthMapPlanes = []
		for i in xrange(0, numPlanes - 1):
			(d, nx, ny, nz) = struct.unpack('<ffff', raw[pos:pos+16])

			self.DepthMapPlanes.append({ 'd': d, 'nx': nx, 'ny': ny, 'nz': nz })
			pos += 16

	def __str__(self):
		tmp = ''
		for x in inspect.getmembers(self):
			if x[0].startswith("__") or inspect.ismethod(x[1]):
				continue
			
			tmp += "%s: %s\n" % x
		return tmp

pano = FindPano(27.683528,-99.580078,2000)
print GetPanoMetadata(pano.PanoId)
