package planning;
import java.io.*;
import java.util.*;
/** A class to represent a set of production technologies in a more compact
  form than as an input output table or matrix. It can take advantage
  of the sparse character of large io tables.
  This extends the base class Technique to add possible co-products
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
public class JointProductionTechnique extends Technique {
    /** note the coproducts are in addition to the main product */
    double [] coproductOutput;
    int [] coproductCodes;
    /** create technique with <p>prinicipal output code PC and
     * <p>grossoutput gO<p>
     * usage of inputs given by usage<p>
     * the identity of the inputs by codes<p>
     * the quantity of coproduct outputs by cooutput<p>
     * the identity of the coproducts by cooutputcodes<p> note the coproducts are in addition to the main product */
    public  JointProductionTechnique (String id, int PC, double gO, double[] usage,int[]codes,double[] cooutput,int[] cooutputcodes) {
        super(id,  PC,   gO,   usage, codes);
        coproductCodes=cooutputcodes;
        coproductOutput=cooutput;
    }
    public String toString() {
        return  "JointProductionTechnique{"+identifier+","+productCode+","+grossOutput+",\n"+
                Arrays.toString(coproductCodes)+",\n"+
                Arrays.toString(coproductOutput)+
                ",\n"+ Arrays.toString(inputCodes)+",\n"+ Arrays.toString(inputUsage)
                +"\n}";
    }
    /** tells you the rate of harmony gain per unit of input where both output and input are
     * measured in contribution to total harmony */
    public double rateOfHarmonyGain(double[]derivativeOfProductHarmony) {
        double gain = derivativeOfProductHarmony[ productCode]* grossOutput;
        double cost=0;
        for(int j=0; j< inputCodes.length; j++) {
            cost+=derivativeOfProductHarmony[ inputCodes[j]]* inputUsage[j];
        }
        for(int j=0; j< coproductCodes.length; j++) {
            gain+=derivativeOfProductHarmony[ coproductCodes[j]]* coproductOutput[j];
        }
        return (gain-cost)/cost;
    }
    /** this returns the marginal physical products of all the co products
     * that result from one extra unit of the input */
    public double[]  marginalphysicalcoproducts(int    input) {
        double[] mpp = new double[coproductCodes.length];
        int pos =findiIna(input,  inputCodes);
        for(int i=0; i < mpp.length; i++) {
            mpp[i]=coproductOutput[i]/inputUsage[pos];
        }
        return mpp;
    }

    public double []getCoproductionQuantities() {
        return coproductOutput;
    }
    public int[] getCoproductionCodes() {
        return coproductCodes;
    }
}
