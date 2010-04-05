#!/usr/bin/python
# -*- coding: utf-8 -*-
import struct
import os
import sys

class BitArray:
	
	def __init__(self, data):
		self._Data = data
		self._ByteIdx = 0
		self._BitIdx = 0
	
	def mask(self, bitcount):
		return (1 << bitcount) - 1
	
	def read(self, bitsleftcount):
		
		result = 0
		bitsdonecount = 0
		while bitsleftcount:
			curbitsleftcount = 8 - self._BitIdx
			curdata = ord(self._Data[self._ByteIdx]) & self.mask(curbitsleftcount)
			
			if curbitsleftcount >= bitsleftcount:
				result <<= bitsleftcount				
				result |= curdata >> (curbitsleftcount - bitsleftcount)
				self._BitIdx += bitsleftcount
				bitsleftcount = 0
			else:
				result <<= curbitsleftcount
				result |= curdata
				bitsleftcount -= curbitsleftcount
				self._BitIdx += curbitsleftcount
			
			if self._BitIdx >= 8:
				self._BitIdx = 0
				self._ByteIdx += 1
				
		return result

f = open('1-b1da0adf-4f77-4070-a88e-bffe1c60529a.COMPRESSION')
(compsize, origsize) =  struct.unpack("<II", f.read(8))
bits = BitArray(f.read())
f.close();
print compsize
print origsize

blocksize = 0
while True:
	if blocksize == 0:
		blocksize = bits.read(16)
		
		extrasetcount = bits.read(5)
		extrasetlengths = ()
		if extrasetcount == 0:
			extrasetlengths = (bits.read(5), )
		else:
			for idx in xrange(0, extrasetcount):
				curval = bits.read(3)
				if curval == 7:
					count = 3
					while bits.read(1):
						count += 1
					curval = count + 4
				extrasetlengths += (curval, )
				
				if idx == 2:
					extrasetlengths+= (0,) * bits.read(2)			
		print extrasetlengths


		charlensetcount = bits.read(9)
		charlensetlengths = ()
		if charlensetcount == 0:
			charlensetlengths = (bits.read(9), )
		else:
			for idx in xrange(0, charlensetcount):
				curval = bits.read(3)
				if curval == 7:
					count = 3
					while bits.read(1):
						count += 1
					curval = count + 4
				extrasetlengths += (curval, )
				
				if idx == 2:
					extrasetlengths+= (0,) * bits.read(2)			
		print charlensetlengths



	sys.exit(1)
	