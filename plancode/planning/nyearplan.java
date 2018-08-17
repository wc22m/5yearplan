package planning;
import java.io.*;
import java.util.*;
/** A programme to construct 5 year or n year socialist plans
 *
 * It produces an output file of the plan in lp-solve format on standard out<p>
 * Usage java planning.nyearplan flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv
 *
 * <p>
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
public class nyearplan {
    static final int flow=0,cap=1,dep=2,targ=3;
    static String [][] rowheads = new String[4][1];
    static String [][]colheads = new String[4][1];
    static double [][][] matrices= new double [4][1][1];
    static double []outputs;
    static double[] labour;
    static int maxprod;
    static int   consistent(String []shorter,String[]longer) { /* return -1 if the lists are consistent */
        if(longer.length<shorter.length)return 0;
        for(int i= 0 ; i<shorter.length; i++) {
            if(shorter[i]==null) return i;
            if(longer[i]==null) return i;
            if (!shorter[i].equals(longer[i]))return i;
        }
        return -1;
    }
    public static void main(String [] args)throws Exception {
        if (args.length !=4 ) {
            System.err.println("Usage java planning.nyearplan flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv");
        } else {
            csvfilereader flowread,capread,depread,labtargread;
            flowread=new csvfilereader(args[flow]);
            pcsv flowtab = flowread.parsecsvfile();
            capread= new csvfilereader(args[cap]);
            pcsv captab = capread.parsecsvfile();
            depread = new csvfilereader(args[dep]);
            pcsv deptab = depread.parsecsvfile();
            labtargread = new csvfilereader(args[targ]);
            pcsv labetctab=labtargread .parsecsvfile();
            if (flowtab == null) {
                throw new Exception(" Error opening or parsing "+args[flow]);
            }
            if (captab == null) {
                throw new Exception(" Error opening or parsing "+args[cap]);
            }
            if (deptab == null) {
                throw new Exception(" Error opening or parsing "+args[dep]);
            }
            if (labetctab == null) {
                throw new Exception(" Error opening or parsing "+args[targ]);
            }

            pcsv[] parsed = {flowtab,captab,deptab,labetctab};
            for (int i=flow ; i<=targ; i++) {
                rowheads[i]=flowread.getrowheaders(parsed[i]);
                colheads[i]=flowread.getcolheaders(parsed[i]);
                matrices[i]=flowread.getdatamatrix(parsed[i]);

                int consistency=consistent(colheads[flow],colheads[i]);
                if(consistency>=0) throw new Exception(" flow table col header inconsistent with header of table "+i
                                                           +"\n"+  colheads[flow][consistency]+" !="+colheads[i][consistency]+" at position "+consistency);
                if(i!= targ) {
                    consistency=consistent(colheads[i],rowheads[i]);
                    if(consistency>=0) throw new Exception("   col header inconsistent with row header for table  "+i
                                                               +"\n"+  colheads[i][consistency]+" !="+rowheads[i][consistency]+" at position "+consistency
                                                               +"\ncolheads="+Arrays.toString(colheads[i])
                                                               +"\nrowheads="+Arrays.toString(rowheads[i]));
                }
            }
            outputs = matrices[flow][outputrowinheaders() ];
            //   System.out.println("labour row is "+labourRow());
            labour = matrices[flow][labourRow()];

            // System.out.println("outputs "+Arrays.toString(outputs));
            // System.out.println("labour "+Arrays.toString(labour));
            // System.out.println("flow matrix "+Arrays.deepToString(matrices[flow]));
            // System.out.println("row headers "+Arrays.deepToString(rowheads));
            int years = countyears(rowheads[targ]);
            maxprod=colheads[flow].length-1;
            int year;
            System.out.println(maximiser(years));
            for (year=1; year<=years; year++) {
                // set a target fiven by leontief demand for year
                System.out.println(targeqn(year));
                System.out.println(labourtotal(year));
                // now print out labour supply constraint
                System.out.println(namelabourfor(year)+"\t<=\t" +matrices[targ][year][labourRow()]+";");;
                for(int product=1; product<=maxprod; product++) {

                    for(int stock =1; stock<=maxprod; stock++) {
                        String eq=outputequationfor(product,stock,year);
                        if(eq !="")System.out.println(eq);
                        eq = flowconstraintfor(product,stock,year);
                        if(eq !="")System.out.println(eq);
                        System.out.println(namedep(product,stock,year)+" =\t"+matrices[dep][stock][product]+" "+namecap(product,stock,year)+";");
                        if (year>1) {
                            System.out.println(accumulationconstraint(product,stock,year));
                        } else { // set initial capital stocks
                            System.out.println(namecap(product,stock,year)+"\t<=\t"+ matrices[cap][stock][product]+";");
                        }
                    }
                    System.out.println(labourconstraintfor(product,year));
                    System.out.println(accumulationtotal(product,year));
                    System.out.println(productiveconsumption(product,year));
                    System.out.println(nameconsumption(product,year)+"\t<=\t"+ nameoutput(product,year) + " - "+nameaccumulation(product ,year)+
                                       "-"+nameproductiveconsumption(product,year)+";");
                }
            }
        }
    }
    static String maximiser(int years) {
        String s="  max:\t"+nametarget(1);
        for (int i =2; i<=years; i++) s+= (" +\t"+nametarget(i));
        return s+";";
    }
    static double gettargnorm(int year) {
        double total=0;
        for(int i=1; i<=maxprod; i++) total = total +(matrices[targ][year][i]*matrices[targ][year][i]);
        return Math.sqrt(total);
    }
    static String targeqn(int year) {

        String s= "";
        for (int i=1; i<=maxprod; i++)if(matrices[targ][year][i]>0)s= s +
                        nametarget(year)+" <=\t"+( 1/matrices[targ][year][i] )+ " "+nameconsumption(i,year)+";\n";
        return s ;
    }
    static String productiveconsumption(int product, int year) {
        String s=nameproductiveconsumption(product,year)+"\t>=\t"+nameflow(1,product,year);
        for (int i=2; i<=maxprod; i++)s= s +" +\t"  +nameflow(i,product,year);
        return s+";";
    }
    static String accumulationtotal(int product, int year) {
        String s=nameaccumulation(product,year)+"\t>=\t"+nameaccumulation(1,product,year);
        for (int i=2; i<=maxprod; i++)s= s +" +\t"  +nameaccumulation(i,product,year);
        return s+";";
    }
    static String labourtotal(  int year) {
        String s=namelabourfor( year)+"\t>=\t"+namelabourfor(1, year);
        for (int i=2; i<=maxprod; i++)s= s +" +\t"  +namelabourfor(i, year);
        return s+";";
    }
    static int outputrowinheaders()throws Exception {
        int i;
        for(i=0; i<rowheads[flow].length; i++)
            if (rowheads[flow][i].equals("output"))return i;
        throw new Exception("No output row in flow matrix");
    }
    static int labourRow()throws Exception {
        int i;
        for(i=0; i<rowheads[flow].length; i++)
            if (rowheads[flow][i].equals("labour"))return i  ;
        throw new Exception("No labour row in flow matrix");
    }
    static String flowconstraintfor(int product, int input, int year) {
        String s=nameoutput(product,year)+"\t<=\t";
        if(matrices[flow][input][product]!=0.0) {
            s=s+ (outputs[product]/matrices[flow][input][product])+" "+nameflow(product,input,year)+";";
        } else {
            s="";
        }
        return s;
    }
    static String outputequationfor(int product,int stock, int year) {
        String s=nameoutput(product,year)+"\t<=\t";
        if(matrices[cap][stock][product]!=0.0) {
            s=s+ (outputs[product]/matrices[cap][stock][product])+" "+namecap(product,stock,year)+";";
        } else {
            s="";
        }
        return s;
    }
    static String labourconstraintfor(int product,  int year)throws Exception {
        String s=nameoutput(product,year)+"\t<=\t";
        s=s+(outputs[product]/labour[product])+ " "+namelabourfor(product,year)+";";
        if(outputs[product]==0.0)s="";
        return s;
    }
    static String namelabourfor( int product, int year) {
        return "labourFor"+colheads[flow][product]+year;
    }
    static String namelabourfor(  int year) {
        return "labourForYear"+ year;
    }
    static String nameoutput(int product, int year) {
        return "outputOf"+colheads[flow][product]+year;
    }
    static String accumulationconstraint(int product, int input, int year) {
        String s=namecap(product,input,year)+"\t<=\t";
        s=s+ namecap(product,input,year-1)+" + "+ nameaccumulation(product,input,year-1)+" -\t"+namedep(product,input,year-1);
        return s+";";
    }
    static String nameaccumulation(int product, int input, int year) {
        return "accumulationFor"+colheads[flow][product]+"Of"+colheads[flow][input]+year;
    }
    static String nameaccumulation(int product,   int year) {
        return "accumulationOf"+colheads[flow][product]+year  ;
    }
    static String nameconsumption(int product,   int year) {
        return "finalConsumptionOf"+colheads[flow][product]+year  ;
    }
    static String nametarget(int year) {
        return "targetFulfillmentForYear"+year;
    }
    static String nameproductiveconsumption(int product,   int year) {
        return "productiveConsumptionOf"+colheads[flow][product]+year  ;
    }
    static String nameflow(int product, int input, int year) {
        return "flowFor"+colheads[flow][product]+"Of"+colheads[flow][input]+year;
    }
    static String namedep(int product, int input, int year) {
        return "depreciationIn"+colheads[flow][product]+"ProductionOf"+colheads[flow][input]+year;
    }
    static String namecap(int product, int input, int year) {
        return "capitalstockFor"+colheads[flow][product]+"MadeUpOf"+colheads[flow][input]+year;
    }

    static int countyears(String[]heads) {
        int j=0,i;
        for(i=0; i<heads.length; i++)
            if(heads[i]!=null)
                if(heads[i].startsWith("year"))j++;
        return j;
    }
}
