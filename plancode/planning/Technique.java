package planning;
import java.io.*;
import java.util.*;
/** A class to represent a set of production technologies in a more compact
  form than as an input output table or matrix. It can take advantage
  of the sparse character of large io tables.
  <p>
       Copyright (C) 2018 William Paul Cockshott

       This program is free software: you can redistribute it and/or modify
       it under the terms of the GNU General Public License as published by
       the Free Software Foundation, either version 3 of the License, or
       (at your option) any later version.

       This program is distributed in the hope that it will be useful,
       but WITHOUT ANY WARRANTY; without even the implied warranty of
       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
       GNU General Public License for more details.

       You should have received a copy of the GNU General Public License
       along with this program.  If not, see https://www.gnu.org/licenses/.
    * */
public class Technique {
    String identifier;
    int  productCode;
    double grossOutput;
    double [] inputUsage;
    int [] inputCodes;

    /** PC is the product code produced<p>
     * gO is the gross output working at standard capacity <p>
     * usage is the amount of each input used at standard capacity<p>
     * codes are the product codes of the inputs being used, stands in one to one
     * correspondence to the usage entries */
    public  Technique (String id, int PC, double gO, double[] usage,int[]codes) {
        identifier=id;
        productCode=PC;
        grossOutput=gO;
        inputUsage=usage;
        inputCodes=codes;
    }

    public int getProductCode() {
        return productCode;
    }
    public double getGrossOutput() {
        return grossOutput;
    }
    public double []getInputUsage() {
        return inputUsage;
    }
    public int[] getInputCodes() {
        return inputCodes;
    }
    public String getIdentifier() {
        return identifier;
    }
    public String toString() {
        return  "Technique{"+identifier+","+productCode+","+grossOutput
                +",\n"+  Arrays.toString(inputUsage)+",\n"+
                Arrays.toString(inputCodes)+"\n}";
    }
};
