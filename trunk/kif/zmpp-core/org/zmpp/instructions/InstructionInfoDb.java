/*
 * Created on 2008/07/23
 * Copyright (c) 2005-2010, Wei-ju Wu.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of Wei-ju Wu nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.zmpp.instructions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.zmpp.vm.Instruction;

/**
 * This is the new representation for the information about instructions in
 * the Z-machine. As opposed to the old xStaticInfo classes, this is a database
 * containing all the information. It can be regarded as static configuration
 * which is compiled into the application.
 * @author Wei-ju Wu
 * @version 1.5
 */
public final class InstructionInfoDb {


  // Commonly used version ranges
  private static final int[] ALL_VERSIONS = {1, 2, 3, 4, 5, 6, 7, 8};
  private static final int[] EXCEPT_V6    = {1, 2, 3, 4, 5, 7, 8};
  private static final int[] V1_TO_V3     = {1, 2, 3};
  private static final int[] V1_TO_V4     = {1, 2, 3, 4};
  private static final int[] V5_TO_V8     = {5, 6, 7, 8};
  private static final int[] V3_TO_V8     = {3, 4, 5, 6, 7, 8};
  private static final int[] V4_TO_V8     = {4, 5, 6, 7, 8};
  private static final int[] V4           = {4};
  private static final int[] V6           = {6};

  /**
   * Information structure about the instruction.
   */
  public static class InstructionInfo {
    private String name;
    private boolean isStore, isBranch, isPrint, isOutput;
    /**
     * Constructor.
     * @param name name
     * @param isBranch branch flag
     * @param isStore store flag
     * @param isPrint print flag
     * @param isOutput output flag
     */
    public InstructionInfo(String name, boolean isBranch, boolean isStore,
                           boolean isPrint, boolean isOutput) {
      this.name = name;
      this.isBranch = isBranch;
      this.isStore = isStore;
      this.isPrint = isPrint;
      this.isOutput = isOutput;
    }
    /**
     * Determine whether this InstructionInfo represents a store.
     * @return true for store, false if not
     */
    public boolean isStore() { return isStore; }
    /**
     * Determine whether this InstructionInfo represents a branch.
     * @return true for branch, false if not
     */
    public boolean isBranch() { return isBranch; }
    /**
     * Determine whether this InstructionInfo represents a print instruction.
     * @return true for print, false if not
     */
    public boolean isPrint() { return isPrint; }
    /**
     * Determine whether this InstructionInfo represents an output instruction.
     * @return true for output, false if not
     */
    public boolean isOutput() { return isOutput; }
    /**
     * Returns the opcode name.
     * @return opcode name
     */
    public String getName() { return name; }
  }

  // Factory methods to create the common InstructionInfo types
  /**
   * Creates standard InstructionInfo object.
   * @param name name
   * @return InstructionInfo object
   */
  private InstructionInfo createInfo(String name) {
    return new InstructionInfo(name, false, false, false, false);
  }
  /**
   * Creates branch-and-store InstructionInfo object.
   * @param name name
   * @return InstructionInfo object
   */
  private InstructionInfo createBranchAndStore(String name) {
    return new InstructionInfo(name, true, true, false, false);
  }
  /**
   * Creates store InstructionInfo object.
   * @param name name
   * @return InstructionInfo object
   */
  private InstructionInfo createStore(String name) {
    return new InstructionInfo(name, false, true, false, false);
  }
  /**
   * Creates branch InstructionInfo object.
   * @param name name
   * @return InstructionInfo object
   */
  private InstructionInfo createBranch(String name) {
    return new InstructionInfo(name, true, false, false, false);
  }
  /**
   * Creates print InstructionInfo object.
   * @param name name
   * @return InstructionInfo object
   */
  private InstructionInfo createPrint(String name) {
    return new InstructionInfo(name, false, false, true, true);
  }
  /**
   * Creates output InstructionInfo object.
   * @param name name
   * @return InstructionInfo object
   */
  private InstructionInfo createOutput(String name) {
    return new InstructionInfo(name, false, false, false, true);
  }

  /** The hashmap to represent the database */
  private Map infoMap =
          new HashMap();

  /**
   * Private constructor.
   */
  private InstructionInfoDb() {
    // 0OP
    addInfoForAll(createInfo("RTRUE"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_RTRUE);
    addInfoForAll(createInfo("RFALSE"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_RFALSE);
    addInfoForAll(createPrint("PRINT"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_PRINT);
    addInfoForAll(createPrint("PRINT_RET"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_PRINT_RET);
    addInfoForAll(createInfo("NOP"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_NOP);
    addInfoFor(createBranch("SAVE"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_SAVE, V1_TO_V3);
    addInfoFor(createBranch("RESTORE"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_RESTORE, V1_TO_V3);
    addInfoFor(createStore("SAVE"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_SAVE, V4);
    addInfoFor(createStore("RESTORE"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_RESTORE, V4);
    addInfoForAll(createInfo("RESTART"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_RESTART);
    addInfoForAll(createInfo("RET_POPPED"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_RET_POPPED);
    addInfoFor(createInfo("POP"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_POP, V1_TO_V4);
    addInfoFor(createStore("CATCH"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_CATCH, V5_TO_V8);
    addInfoForAll(createInfo("QUIT"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_QUIT);
    addInfoForAll(createOutput("NEW_LINE"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_NEW_LINE);
    addInfoFor(createInfo("SHOW_STATUS"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_SHOW_STATUS,
               new int[] {3});
    addInfoFor(createBranch("VERIFY"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_VERIFY,
               new int[] {3, 4, 5, 6, 7, 8});
    addInfoFor(createInfo("PIRACY"), Instruction.OPERANDCOUNT_C0OP, Instruction.C0OP_PIRACY, V5_TO_V8);

    // 1OP
    addInfoForAll(createBranch("JZ"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_JZ);
    addInfoForAll(createBranchAndStore("GET_SIBLING"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_GET_SIBLING);
    addInfoForAll(createBranchAndStore("GET_CHILD"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_GET_CHILD);
    addInfoForAll(createStore("GET_PARENT"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_GET_PARENT);
    addInfoForAll(createStore("GET_PROP_LEN"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_GET_PROP_LEN);
    addInfoForAll(createInfo("INC"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_INC);
    addInfoForAll(createInfo("DEC"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_DEC);
    addInfoForAll(createOutput("PRINT_ADDR"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_PRINT_ADDR);
    addInfoFor(createStore("CALL_1S"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_CALL_1S, V4_TO_V8);
    addInfoForAll(createInfo("REMOVE_OBJ"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_REMOVE_OBJ);
    addInfoForAll(createOutput("PRINT_OBJ"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_PRINT_OBJ);
    addInfoForAll(createInfo("RET"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_RET);
    addInfoForAll(createInfo("JUMP"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_JUMP);
    addInfoForAll(createOutput("PRINT_PADDR"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_PRINT_PADDR);
    addInfoForAll(createStore("LOAD"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_LOAD);
    addInfoFor(createStore("NOT"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_NOT, V1_TO_V4);
    addInfoFor(createInfo("CALL_1N"), Instruction.OPERANDCOUNT_C1OP, Instruction.C1OP_CALL_1N, V5_TO_V8);

    // 2OP
    addInfoForAll(createBranch("JE"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_JE);
    addInfoForAll(createBranch("JL"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_JL);
    addInfoForAll(createBranch("JG"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_JG);
    addInfoForAll(createBranch("DEC_CHK"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_DEC_CHK);
    addInfoForAll(createBranch("INC_CHK"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_INC_CHK);
    addInfoForAll(createBranch("JIN"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_JIN);
    addInfoForAll(createBranch("TEST"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_TEST);
    addInfoForAll(createStore("OR"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_OR);
    addInfoForAll(createStore("AND"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_AND);
    addInfoForAll(createBranch("TEST_ATTR"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_TEST_ATTR);
    addInfoForAll(createInfo("SET_ATTR"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_SET_ATTR);
    addInfoForAll(createInfo("CLEAR_ATTR"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_CLEAR_ATTR);
    addInfoForAll(createInfo("STORE"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_STORE);
    addInfoForAll(createInfo("INSERT_OBJ"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_INSERT_OBJ);
    addInfoForAll(createStore("LOADW"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_LOADW);
    addInfoForAll(createStore("LOADB"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_LOADB);
    addInfoForAll(createStore("GET_PROP"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_GET_PROP);
    addInfoForAll(createStore("GET_PROP_ADDR"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_GET_PROP_ADDR);
    addInfoForAll(createStore("GET_NEXT_PROP"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_GET_NEXT_PROP);
    addInfoForAll(createStore("ADD"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_ADD);
    addInfoForAll(createStore("SUB"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_SUB);
    addInfoForAll(createStore("MUL"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_MUL);
    addInfoForAll(createStore("DIV"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_DIV);
    addInfoForAll(createStore("MOD"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_MOD);
    addInfoFor(createStore("CALL_2S"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_CALL_2S, V4_TO_V8);
    addInfoFor(createInfo("CALL_2N"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_CALL_2N, V5_TO_V8);
    addInfoFor(createInfo("SET_COLOUR"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_SET_COLOUR, V5_TO_V8);
    addInfoFor(createInfo("THROW"), Instruction.OPERANDCOUNT_C2OP, Instruction.C2OP_THROW, V5_TO_V8);

    // VAR
    addInfoFor(createStore("CALL"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_CALL, V1_TO_V3);
    addInfoFor(createStore("CALL_VS"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_CALL_VS, V4_TO_V8);
    addInfoForAll(createInfo("STOREW"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_STOREW);
    addInfoForAll(createInfo("STOREB"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_STOREB);
    addInfoForAll(createInfo("PUT_PROP"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_PUT_PROP);
    addInfoFor(createInfo("SREAD"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_SREAD, V1_TO_V4);
    addInfoFor(createStore("AREAD"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_AREAD, V5_TO_V8);
    addInfoForAll(createOutput("PRINT_CHAR"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_PRINT_CHAR);
    addInfoForAll(createOutput("PRINT_NUM"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_PRINT_NUM);
    addInfoForAll(createStore("RANDOM"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_RANDOM);
    addInfoForAll(createInfo("PUSH"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_PUSH);
    addInfoFor(createInfo("PULL"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_PULL, EXCEPT_V6);
    addInfoFor(createStore("PULL"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_PULL, V6);
    addInfoFor(createOutput("SPLIT_WINDOW"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_SPLIT_WINDOW,
               V3_TO_V8);
    addInfoFor(createInfo("SET_WINDOW"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_SET_WINDOW, V3_TO_V8);
    addInfoFor(createStore("CALL_VS2"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_CALL_VS2, V4_TO_V8);
    addInfoFor(createOutput("ERASE_WINDOW"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_ERASE_WINDOW,
               V4_TO_V8);
    addInfoFor(createOutput("ERASE_LINE"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_ERASE_LINE, V4_TO_V8);
    addInfoFor(createInfo("SET_CURSOR"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_SET_CURSOR, V4_TO_V8);
    addInfoFor(createInfo("GET_CURSOR"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_GET_CURSOR, V4_TO_V8);
    addInfoFor(createInfo("SET_TEXT_STYLE"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_SET_TEXT_STYLE,
               V4_TO_V8);
    addInfoFor(createInfo("BUFFER_MODE"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_BUFFER_MODE,
               V4_TO_V8);
    addInfoFor(createInfo("OUTPUT_STREAM"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_OUTPUT_STREAM,
               V3_TO_V8);
    addInfoFor(createInfo("INPUT_STREAM"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_INPUT_STREAM,
               V3_TO_V8);
    addInfoFor(createInfo("SOUND_EFFECT"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_SOUND_EFFECT,
               V3_TO_V8);
    addInfoFor(createStore("READ_CHAR"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_READ_CHAR, V4_TO_V8);
    addInfoFor(createBranchAndStore("SCAN_TABLE"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_SCAN_TABLE,
               V4_TO_V8);
    addInfoFor(createStore("NOT"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_NOT, V5_TO_V8);
    addInfoFor(createInfo("CALL_VN"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_CALL_VN, V5_TO_V8);
    addInfoFor(createInfo("CALL_VN2"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_CALL_VN2, V5_TO_V8);
    addInfoFor(createInfo("TOKENISE"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_TOKENISE, V5_TO_V8);
    addInfoFor(createInfo("ENCODE_TEXT"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_ENCODE_TEXT, V5_TO_V8);
    addInfoFor(createInfo("COPY_TABLE"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_COPY_TABLE, V5_TO_V8);
    addInfoFor(createOutput("PRINT_TABLE"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_PRINT_TABLE, V5_TO_V8);
    addInfoFor(createBranch("CHECK_ARG_COUNT"), Instruction.OPERANDCOUNT_VAR, Instruction.VAR_CHECK_ARG_COUNT,
               V5_TO_V8);

    // EXT
    addInfoFor(createStore("SAVE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_SAVE, V5_TO_V8);
    addInfoFor(createStore("RESTORE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_RESTORE, V5_TO_V8);
    addInfoFor(createStore("LOG_SHIFT"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_LOG_SHIFT, V5_TO_V8);
    addInfoFor(createStore("ART_SHIFT"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_ART_SHIFT, V5_TO_V8);
    addInfoFor(createStore("SET_FONT"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_SET_FONT, V5_TO_V8);
    addInfoFor(createOutput("DRAW_PICTURE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_DRAW_PICTURE, V6);
    addInfoFor(createBranch("PICTURE_DATA"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_PICTURE_DATA, V6);
    addInfoFor(createOutput("ERASE_PICTURE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_ERASE_PICTURE, V6);
    addInfoFor(createInfo("SET_MARGINS"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_SET_MARGINS, V6);
    addInfoFor(createStore("SAVE_UNDO"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_SAVE_UNDO, V5_TO_V8);
    addInfoFor(createStore("RESTORE_UNDO"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_RESTORE_UNDO,
               V5_TO_V8);
    addInfoFor(createOutput("PRINT_UNICODE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_PRINT_UNICODE,
               V5_TO_V8);
    addInfoFor(createInfo("CHECK_UNICODE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_CHECK_UNICODE,
               V5_TO_V8);
    addInfoFor(createOutput("MOVE_WINDOW"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_MOVE_WINDOW, V6);
    addInfoFor(createInfo("WINDOW_SIZE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_WINDOW_SIZE, V6);
    addInfoFor(createInfo("WINDOW_STYLE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_WINDOW_STYLE, V6);
    addInfoFor(createStore("GET_WIND_PROP"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_GET_WIND_PROP, V6);
    addInfoFor(createOutput("SCROLL_WINDOW"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_SCROLL_WINDOW, V6);
    addInfoFor(createInfo("POP_STACK"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_POP_STACK, V6);
    addInfoFor(createInfo("READ_MOUSE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_READ_MOUSE, V6);
    addInfoFor(createInfo("MOUSE_WINDOW"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_MOUSE_WINDOW, V6);
    addInfoFor(createBranch("PUSH_STACK"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_PUSH_STACK, V6);
    addInfoFor(createInfo("PUT_WIND_PROP"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_PUT_WIND_PROP, V6);
    addInfoFor(createOutput("PRINT_FORM"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_PRINT_FORM, V6);
    addInfoFor(createBranch("MAKE_MENU"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_MAKE_MENU, V6);
    addInfoFor(createInfo("PICTURE_TABLE"), Instruction.OPERANDCOUNT_EXT, Instruction.EXT_PICTURE_TABLE, V6);
  }

  /**
   * Adds the specified info struct for all Z-machine versions.
   * @param info the InstructionInfo
   * @param opCount the OperandCount
   * @param opcodeNum the opcode number
   */
  private void addInfoForAll(InstructionInfo info, int opCount,
                             int opcodeNum) {
    addInfoFor(info, opCount, opcodeNum, ALL_VERSIONS);
  }

  /**
   * Adds the specified InstructionInfo for the specified Z-machine versions.
   * @param info the InstructionInfo
   * @param opCount the OperandCount
   * @param opcodeNum the opcode number
   * @param versions the valid versions
   */
  private void addInfoFor(InstructionInfo info, int opCount,
                          int opcodeNum, int[] versions) {
    for(int i=0; i<versions.length; i++) {
      infoMap.put(createKey(opCount, opcodeNum, versions[i]), info);
    }
  }

  private static InstructionInfoDb instance = new InstructionInfoDb();

  /**
   * Returns the Singleton instance of the database.
   * @return the database instance
   */
  public static InstructionInfoDb getInstance() { return instance; }

  /**
   * Creates the hash key for the specified instruction information.
   * @param opCount the operand count
   * @param opcodeNum the opcode number
   * @param version the story version
   * @return the key
   */
  private String createKey(int opCount, int opcodeNum, int version) {
    return "" + opCount + ":" + opcodeNum + ":" + version;
  }

  /**
   * Returns the information struct for the specified instruction.
   * @param opCount the operand count
   * @param opcodeNum the opcode number
   * @param version the story version
   * @return the instruction info struct
   */
  public InstructionInfo getInfo(int opCount, int opcodeNum,
                                       int version) {
    //System.out.println("GENERATING KEY: " +
    //                   createKey(opCount, opcodeNum, version));
    return (InstructionInfo) infoMap.get(createKey(opCount, opcodeNum, version));
  }

  /**
   * Determines if the specified operation is valid.
   * @param opCount the operand count
   * @param opcodeNum the opcode number
   * @param version the story version
   * @return true if valid, false otherwise
   */
  public boolean isValid(int opCount, int opcodeNum,
                         int version) {
    return infoMap.containsKey(createKey(opCount, opcodeNum, version));
  }

  /**
   * Prints the keys in the info map.
   */
  public void printKeys() {
    System.out.println("INFO MAP KEYS: ");
    Iterator it = infoMap.keySet().iterator();
    while(it.hasNext()) {
      String key = (String) it.next();
      if (key.startsWith("C1OP:0")) System.out.println(key);
    }
  }
}
