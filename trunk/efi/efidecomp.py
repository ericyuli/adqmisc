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

		# Load in the canonical Huffman bit length table
		extrasetcount = bits.read(5)
		extraset = []
		extrasetvalue = None
		if extrasetcount == 0:
			extrasetvalue = bits.read(5)
		else:
			# Decode the horrible bit length encoding thing!
			idx = 0
			while idx < extrasetcount:
				curval = bits.read(3)
				if curval == 7:
					count = 3
					while bits.read(1):
						count += 1
					curval = count + 4
				extraset += ([idx, curval, None], )
				idx += 1
				
				# decode the extra special nasty hack!
				if idx == 3:
					idxoffset = bits.read(2)
					for zerofillidx in xrange(3, 3 + idxoffset):
						extraset += ([zerofillidx, 0, None], )
					idx += idxoffset

			# Pad with zero entries as necessary
			for zerofillidx in xrange(extrasetcount, 19):
				extraset += ([zerofillidx, 0, None], )

			# Now, sort them by bit length
			extraset = sorted(extraset, key=lambda length: length[1])

			# Allocate huffman codes to the length-ordered symbols
			code = 0
			for idx in xrange(0, len(extraset)):
				if extraset[idx][1] == 0:
					continue

				extraset[idx][2] = hex(code)
				if idx < len(extraset)-1:
					code = (code + 1) << (extraset[idx+1][1] - extraset[idx][1])

			print extraset
		sys.exit(0)


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
					charlensetlengths+= (0,) * bits.read(2)			
		print charlensetlengths



	sys.exit(1)
	