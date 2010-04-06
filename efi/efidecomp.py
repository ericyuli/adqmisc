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

def LoadHuffmanSyms(bits, symscountbits, zeroskipidx):
	huffsyms = None
	symscount = bits.read(symscountbits)
	if symscount == 0:
		v = bits.read(5)
		huffsyms = [ [ v, 1, 0], [ v, 1, 1 ] ]
	else:
		# Decode the horrible bit length encoding thing!
		huffsyms = []
		idx = 0
		while idx < symscount:
			bitlen = bits.read(3)
			if bitlen == 7:
				while bits.read(1):
					bitlen += 1
			if bitlen != 0:
				huffsyms += ([idx, bitlen, None], )
			idx += 1
			
			# decode the extra special nasty zero-skip hack!
			if idx == zeroskipidx:
				idx += bits.read(2)

		# Now, sort them by bit length
		huffsyms = sorted(huffsyms, key=lambda length: length[1])

		# Allocate huffman codes to the length-ordered symbols
		huffsyms[0][2] = 0
		for idx in xrange(1, len(huffsyms)):
			huffsyms[idx][2] = (huffsyms[idx-1][2] + 1) << (huffsyms[idx][1] - huffsyms[idx-1][1])

	return huffsyms

def BuildHuffmanTree(huffsyms):
	hufftree = [None, None]
	for huffsym in huffsyms:
		symbol = huffsym[0]
		bitlen = huffsym[1]
		huffcode = huffsym[2]
		if bitlen == 0:
			continue

		huffsubtree = hufftree
		for bit in xrange(0, bitlen):
			lr = huffcode & (1 << (bitlen - bit - 1)) != 0

			if bit < bitlen - 1:
				if huffsubtree[lr] == None:
					huffsubtree[lr] = [None, None]
				huffsubtree = huffsubtree[lr]
			else:
				huffsubtree[lr] = symbol
	return hufftree





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

		extra_hufftree = BuildHuffmanTree(LoadHuffmanSyms(bits, 5, 3))
		# FIXME: load the char/len set
#		positionset_hufftree = BuildHuffmanTree(LoadHuffmanSyms(bits, 4, -1))
		
		print extra_hufftree
			
		sys.exit(0)


		charlensetcount = bits.read(9)
		charlensetlengths = ()
		if charlensetcount == 0:
			charlensetlengths = (bits.read(9), )
		else:
			for idx in xrange(0, charlensetcount):
				curval = bits.read(3)
				if curval == 7:
					count = extraset
					while bits.read(1):
						count += 1
					curval = count + 4
				extrasetlengths += (curval, )
				
				if idx == 2:
					charlensetlengths+= (0,) * bits.read(2)			
		print charlensetlengths



	sys.exit(1)
	