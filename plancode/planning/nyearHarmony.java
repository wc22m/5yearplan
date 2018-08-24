package planning;
import java.io.*;
import java.util.*;
/** Another programme to construct 5 year or n year socialist plans
 *<p>
 * It uses the Harmony algorithm to solve the plan<p>
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
public class nyearHarmony extends nyearplan {
    static int years;
    static   TechnologyComplex C ;
    static int[][] relativecapnum;// since the matrix is sparse we allocate a distinct product
    // type to each non zero capital stock in the matrix
    // this is done because you can not in general assume that means of production
    // already invested in an industry can be transfered in later years to another industry
    static int [] capnumtoflownum;// a vector that works back to the type of flow needed to make
    // each differentiated stock of capital
    static double []  compressedDeprates;
    static int[][] yearXproductIntensityIndex;
    static int caps;
    public static void main(String [] args)throws Exception {
        if (args.length !=4 ) {
            System.err.println("Usage java planning.nyearHarmony flowmatrix.csv capitalmatrix.csv depreciationmatrix.csv laboursupplyandtargets.csv");
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
            // go through the targets matrix and make sure no targets are actually zero - make them very small positive amounts
            for(int i=0; i<matrices[targ].length; i++)
                for(int j=0; j<matrices[targ][i].length; j++)
                    if(matrices[targ][i][j]==0)matrices[targ][i][j]=1 - Harmonizer.capacitytarget;
            outputs = matrices[flow][outputrowinheaders() ];
            labour = matrices[flow][labourRow()];
            years = countyears(rowheads[targ]);
            maxprod=colheads[flow].length-1;
            yearXproductIntensityIndex=new int[years+1][maxprod+2];
            int year;
            caps= countnonzero(matrices[cap]);
            // work out how many products the harmonizer will have to solve for
            // assume that we have N columns in our table and y years then
            // we have Ny year product combinations
            // in addition we have y labour variables
            // and caps.y capital stocks
            // so the total is y(caps+N+1)

            C =new TechnologyComplex ((maxprod+1+caps)*years );
            //    writeln("productnum "+C.productCount()+" years "+years);
            // Assign identifiers to the outputs

            for(int i=1; i<=maxprod+1; i++)
                for (  year=1; year<=years; year++) {

                    C.setProductName( flownum(i,year), productName(i,year,flownum(i,year)) );

                }
            for(int i=1; i<=maxprod ; i++)// name capital stocks
                for(int j=1; j<=maxprod; j++)
                    for (  year=1; year<=years; year++)
                        if (matrices[cap][i][j] >0) {
                            C.setProductName(capnum(relativecapnum[i][j],year,caps), "C["+i+"]["+j+"]"+year );
                        }



            for (year=1; year<=years; year++) {
                // add a production technology for each definite product
                for(int i=1; i<=maxprod; i++)
                {   double [] usage = new double[countinputsTo(i)];
                    int  [] codes = new int[countinputsTo(i)];
                    int j=0;
                    int index;
                    for(int k=1; k<=maxprod+1; k++) {
                        if (matrices[flow][k][i]>0) {
                            double flows=matrices[flow][k][i];
                            usage[j]= flows;
                            codes[j]=flownum(k,year);
                            j++;
                        }
                        if( k<=maxprod)// no labour row for the capital matrix so we miss last row
                            if (matrices[cap][k][i]>0) {
                                usage[j]= matrices[cap][k ][i ];
                                codes[j]=capnum(relativecapnum[k][i],year,caps);
                                j++;
                            }
                    }
                    Technique t=new Technique (  productName(i,year,flownum(i,year)%maxprod), index=flownum(i,year), outputs[i],   usage, codes);
                    C.addTechnique(t);
                    yearXproductIntensityIndex[year][i]=C.techniqueCount()-1;
                }
                // now add a joint production technique for each type of capital accumulation except for the last year
                if(year<years) {
                    for(int i=0; i<caps ; i++) {
                        double [] usage = {1};// one unit of an input produces one unit of acc in next year
                        int  [] codes =  {flownum(capnumtoflownum[i],year)};// always uses current product for this year
                        int mainoutput=capnum(i,year+1,caps);
                        double grossout = 1;
                        if (year == years-1) {
                            // penultimate year gets a simple technique
                            //   writeln("i"+i);writeln("capnumtoflownum[i]"+capnumtoflownum[i]);writeln("flownum(capnumtoflownum[i],year)"+flownum(capnumtoflownum[i],year));
                            Technique t=new Technique ("A"+C.productIds[mainoutput]+"of"+C.productIds[codes[0]], mainoutput,grossout,   usage, codes);
                            C.addTechnique(t);
                        } else {
                            // other years get a joint production method
                            double [] coproduction= new double[years-year-1];
                            int [] cocodes= new int[years-year-1];
                            for (int k=0; k<cocodes.length; k++) {
                                cocodes[k]=capnum(i,year+k+2,caps);
                                coproduction[k]=Math.pow(1-deprate(i),k+1);// depreciate added on capital stock in future years
                            }
                            Technique t=new JointProductionTechnique("A"+C.productIds[mainoutput]+"of"+C.productIds[codes[0]],mainoutput,grossout,usage,codes,coproduction,cocodes);
                            C.addTechnique(t);
                        }
                    }
                }

            }
            for (Technique t:C.techniques) {
                //    writeln(t.toString());
            }

            // now set up the initial resource vector
            double []initialResource = new double[C.productCount()];
            // put in each years labour
            int lr = labourRow();
            for(int y=1; y<=years; y++) {
                initialResource [flownum(lr,y)]=matrices[targ][y][labourRow()];
                C.nonproduced[flownum(lr,y)]=true;
                C.nonfinal[flownum(lr,y)]=true;
            }

            // put in each years initial capital stock allowing for depreciation
            for(int i=1; i<matrices[cap].length; i++) {

                for(int j=1; j<matrices[cap][i].length; j++) {
                    if(matrices[cap][i][j]>0)
                        for(int y=1; y<=years; y++) {
                            int cn=capnum(relativecapnum[i][j],y,caps);
                            if(y==1)C.nonproduced[cn]=true;
                            C.nonfinal[cn]=true;
                            initialResource [cn]=matrices[cap][ i][j]* Math.pow(1-matrices[dep][i][j],y-1);
                        }
                }
            }


            // now set up the target vector
            double []targets = new double[C.productCount()];
            // initialise to very small numbers to prevent divide by zero

            for(int i=0; i<targets.length; i++)targets[i]=0.03;
            for(int y=1; y<=years; y++)
                for(int j=1; j<matrices[targ][y].length-1; j++)  // do not include the labour col of the targets
                    targets[flownum(j,y)]=matrices[targ][y][j];
            if( Harmonizer.verbose)
                writeln(C.allheadings());
            double [] intensities= Harmonizer.balancePlan(  C, targets,  initialResource   );
            printResults(C, intensities, initialResource);

        }
    }

    static void printResults(TechnologyComplex C, double [] intensity,double[] initialResource)throws Exception {
        double [] netoutput=Harmonizer.computeNetOutput(C,intensity,initialResource);
        double []gross=Harmonizer.computeGrossAvail(  C,   intensity, initialResource);
        writeln("iter,	useweight,	phase2,	temp");
        writeln(" "+Harmonizer.iters+","+Harmonizer.useweight+","+Harmonizer.phase2adjust+","+Harmonizer.startingtemp);
        write("year");
        writeln(colheads[flow]);
        double toth=0;
        for(int year=1; year<=years; year++) {
            writeln(""+year+",flow matrix");
            for(int row=1; row<=outputrowinheaders(); row++) {
                write(""+year);
                write(","+rowheads[flow][row]);
                for(int col=1; col<colheads[flow].length; col++) {
                    int index = yearXproductIntensityIndex[year][col];
                    double howmuch = intensity[index];
                    write(","+howmuch*matrices[flow][row][col]);
                }
                writeln("");
            }

            write(""+year+",");
            write("netoutput ");
            for(int col=1; col<colheads[flow].length; col++) {
                write (","+netoutput[flownum(col,year)]);
            }
            writeln("");
            write(""+year+",");
            write("target  ");
            for(int col=1; col<colheads[flow].length; col++) {
                write (","+matrices[targ][year][col]);
            }
            writeln("");
            write(""+year+",");
            write("netoutput/target  ");
            for(int col=1; col<colheads[flow].length; col++) {
                write (","+(netoutput[flownum(col,year)]/matrices[targ][year][col]));
            }
            writeln("");

            write(""+year+",");
            write("harmony  ");
            for(int col=1; col<colheads[flow].length; col++) {
                double h=Harmony.H(matrices[targ][year][col],netoutput[flownum(col,year)]);
                write (","+h);
                toth+=h;
            }
            writeln("");
            writeln(""+year+",capital use matrix");
            for(int row=1; row<labourRow(); row++) {
                write(""+year);
                write(","+rowheads[flow][row]);
                for(int col=1; col<colheads[flow].length; col++) {
                    int index = yearXproductIntensityIndex[year][col];
                    double howmuch = intensity[index];
                    write(","+howmuch*matrices[cap][row][col]);
                }
                writeln("");
            }
            writeln("");
        }
        writeln("totalharmony ,"+toth);
    }
    static double deprate(int capitaltype)// capitaltype is the compressed capital index
    {
        return compressedDeprates[capitaltype];
    }
    static int countinputsTo(int industry) {
        int total=0;

        for (int i=1; i<=maxprod+1; i++)
            if(matrices[flow][i][industry]>0)total++;
        for (int i=1; i<=maxprod; i++)
            if(matrices[cap][i][industry]>0)total++;
        return total;
    }
    static int countnonzero(double [][]m) {
        int t=0;
        relativecapnum=new int[m.length][m[0].length];
        for(int i=0; i<m.length; i++)
            for(int j=0; j<m[i].length; j++) {
                if(m[i][j]>0) {
                    relativecapnum[i][j]=t;
                    t++;
                }
            }

        capnumtoflownum=new int[t];
        compressedDeprates=new double[t];
        // pass through again filling in the backwardvector
        t=0;
        for(int i=0; i<m.length; i++)
            for(int j=0; j<m[i].length; j++)
                if(m[i][j]>0) {
                    capnumtoflownum[t]=i;
                    compressedDeprates[t]=matrices[dep][i][j];
                    t++;
                }
        return t;
    }
    static void  writeln(String s) {
        System.out.println(s);
    } static void  write (String s) {
        System.out.print (s);
    }
    static void  writeln(boolean []d) {
        for(int i=0; i<d.length; i++)System.out.print(" ,"+d[i]);
        writeln("");
    }
    static void  writeln(double []d) {
        for(int i=0; i<d.length; i++)System.out.printf("%5.4f,",d[i]);
        writeln("");
    }
    static void  writeln(String []s) {
        for(int i=0; i<s.length; i++)System.out.printf(","+s[i]);
        writeln("");
    } static void  write(double []d) {
        for(int i=0; i<d.length; i++)System.out.printf("%5.4f,",d[i]);

    }
    static void  write(String []s) {
        for(int i=0; i<s.length; i++)System.out.printf(","+s[i]);

    }
    static int flownum(int prod, int year) {
        return  (prod-1)+(year-1)*(maxprod+1);
    }
    static int capnum(int prod, int year,int maxcap) {
        return (prod)+(year-1)*(maxcap)+years*(maxprod+1);
    }
    static String capName(int prod, int year,int r,int c) {
        return "capitalstockfor"+C.productIds[flownum(c,year)]+"madeupof"+C.productIds[flownum(c,year)]+year;
    } static String capName(int prod, int year ) {
        return "C"+prod+ "Y"+year ;
    }
    static String productName(int prod, int year,int internalcode) {
        return rowheads[flow][prod]+year;//+"Y"+year+"{"+internalcode+"}";
    }
}
