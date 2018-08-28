package planning;
import java.io.*;
import java.util.*;
/** A class to optimise a set of linear production technologies to meet
a Kantorovich style output target and having a pregiven set of initial resources.<p>
 *
 * It produces an output file of the plan in lp-solve format on standard out<p>
 *  Class to provide optimisation of plans using the algorithm in Towards a New Socialism
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
public class Harmonizer {
    static final double useweight = 5;
    static final double phase2adjust = 0.3 ;
    static final double capacitytarget=0.98;
    static final double startingtemp=0.23;
    static double meanh=0;
    static boolean phase1rescale=true;
    static boolean phase2rescale=true;
    static int iters=80;
    static boolean verbose=false;
    static double [] productHarmony= {};
    /** C is a technology complex, fixed resources should be added as nonproduced products<p>
     * planTargets is the target output of each product<p>
     * returns a vector of technology intensities*/
    public static double [] balancePlan(TechnologyComplex C, double[] planTargets, double [] initialresource )throws Exception {
        if(planTargets.length != C.productCount())
            throw new InconsistentScale(
                "plan target has length "+planTargets.length+" but the number of products in TechnologyComplex is "+C.techniqueCount()
            );
        Vector< Vector<Integer>> producerIndex=C.buildProducerIndex();
        double [] intensity = new double[C.techniqueCount()];
        initialiseIntensities(intensity,C,initialresource);

        double t=startingtemp;
        productHarmony=new double[C.productCount()];
        for(int i=0; i<iters; i++) {
            double [] netOutput = computeNetOutput(C,intensity,initialresource);


            for(int k=0; k<netOutput.length; k++)
                //   if(!C.nonfinal[k])
                productHarmony[k]=   ( Harmony.H(planTargets[k],netOutput[k]));
            meanh=mean(productHarmony,C);

            double [] productHarmonyDerivatives = computeHarmonyDerivatives(netOutput,planTargets,C,intensity );

            adjustIntensities(intensity,
                              productHarmonyDerivatives,
                              t,
                              C,
                              productHarmony,
                              producerIndex,
                              initialresource,planTargets);
            if(verbose)
//if(i==(iters-1))
                printstate(  intensity,  C,  initialresource,   planTargets);
        }
        return intensity;
    }/** compute the derivatives of the harmonies of all products with repect to marginal increase in output in terms of
    actual output units not intensities */
    static  double []  computeHarmonyDerivatives(double[] netOutput,double[] planTargets,TechnologyComplex C,double[] intentsity ) {
        double []dh=new double[netOutput.length];
        for (int i=0; i<dh.length; i++) {
            dh[i]= Harmony.dH(planTargets[i],netOutput[i]);
        }
        for(int solve=0; solve<2; solve++)
            for (int i=0; i<dh.length; i++)
                if(C.nonfinal[i]) {// weighted average of derivative due to shortage and due to potential other use
                    dh[i]= (dh[i]+ useweight* nonfinalHarmonyDerivativeMax(netOutput,i,dh,  C )   )/(useweight+1);
                }
        return dh;
    }
    static  double []  computeHarmony (double[] netOutput,double[] planTargets,TechnologyComplex C,double[] intentsity ) {
        double []h=new double[netOutput.length];
        for (int i=0; i<h.length; i++) {
            h[i]= Harmony.H(planTargets[i],netOutput[i]);
        }

        return h;
    }
    static void printstate(double[]intensity,TechnologyComplex C, double []initial, double [] targets) {
        double [] netOutput = computeNetOutput(C,intensity,initial );
        double [] h =computeHarmony(netOutput,targets,C,intensity );
        double [] hd = computeHarmonyDerivatives(netOutput,targets,C,intensity );
        printstateS(netOutput, hd,h,C,intensity);
    }
    static void printstateS(double[] netOutput,double[]productHarmonyDerivatives,double[]productHarmony,TechnologyComplex C,double[]intensity) {
        System.out.print("netoutput or intensity,");
        write(netOutput);
        writeln(intensity);
        System.out.print("h ,");
        writeln(productHarmony);

        System.out.print("dh/dp or gainrate,");
        write (productHarmonyDerivatives);
        double[] expansionrate=new double[C.techniques.size()];
        for(int i=0; i<C.techniques.size(); i++)
        {
            Technique t= C.techniques.elementAt(i);
            expansionrate[i] =1+sigmoid( t.rateOfHarmonyGain(productHarmonyDerivatives)) *startingtemp*phase2adjust ;
        }
        writeln(expansionrate);
    }
    static void  writeln(String s) {
        System.out.println(s);
    }
    static void  writeln(double []d) {
        for(int i=0; i<d.length; i++)System.out.printf("%5.4f,",d[i]);
        writeln("");
    } static void  write (double []d) {
        for(int i=0; i<d.length; i++)System.out.printf("%5.4f,",d[i]);

    }
    /** for non final goods we make derivatives their harmonies the maximum of the derivatives of the harmonies of their users */
    static double nonfinalHarmonyDerivativeMax(double[] netOutput,int nonfinal,double [] dharmonies,TechnologyComplex C ) {
        Vector< Vector<Integer>> userIndex;
        userIndex=C.buildUserIndex();
        double max,total;
        max= -1e22;
        total=0;
        int best;
        best=0;
        Vector<Integer> users = userIndex.elementAt(nonfinal);
        for(int i=0; i<users.size(); i++) {
            int techno=users.elementAt(i);
            Technique t= C.techniques.elementAt(techno);
            int produces =t.getProductCode();
            double dhp= dharmonies[produces];
            double d= dhp*marginalphysicalproduct(  techno,   nonfinal, C );
            // if it is a joint producing technology it will have harmony contributions from the coproducts
            if (t instanceof JointProductionTechnique) {
                JointProductionTechnique J=(JointProductionTechnique)t;
                double[] mpp= J. marginalphysicalcoproducts(nonfinal);
                int[] codes= J.getCoproductionCodes();
                for (int j=0; j<mpp.length; j++)
                    d+= dharmonies[codes[j]]*mpp[j];
            }
            total +=d;
            if((d)>max) {
                max=d;

            }
        }
        return total/users.size();
        // return max;
    }
    /** marginal physical product of technology techno with respect to the input */
    static double marginalphysicalproduct(int techno, int input, TechnologyComplex C ) {
        Technique user=C.techniques.elementAt(techno);
        return user.marginalphysicalproduct(input);

    }
    static double mean(double[] m,TechnologyComplex C ) {
        double sum = 0;
        int num=0;
        for (int i = 0; i < m.length; i++)
            if(!C.nonproduced[i]) {
                sum += m[i];
                num++;
            }
        return sum / num;
    }
    static double sdev(double[] m, TechnologyComplex C) {
        double sum = 0;
        double av= mean(m,C);
        int num=0;
        for (int i = 0; i < m.length; i++) if(!C.nonproduced[i]) {
                sum += (m[i]-av)*(m[i]-av) ;
                num++;
            }
        return Math.sqrt(sum / num);
    }
    static double mean(double[] m  ) {
        double sum = 0;
        int num=0;
        for (int i = 0; i < m.length; i++)
        {
            sum += m[i];
            num++;
        }
        return sum / num;
    }
    static double sdev(double[] m,double av) {
        double sum = 0;
        int num=0;
        for (int i = 0; i < m.length; i++) {
            sum += (m[i]-av)*(m[i]-av) ;
            num++;
        }
        return Math.sqrt(sum / num);
    }
    /** shrink or expand all industries in order to not exceed target level of use of the critical fixed reource */
    static void rescaleIntensity(double[]intense,TechnologyComplex C, double [] initialresource) {
        double [] netoutput=computeNetOutput(C,intense,initialresource);
        double maxfrac=0;

        for (int i=0; i<netoutput.length; i++) if(C.nonproduced[i]) {
                double resource = initialresource[i];
                double usage =resource- netoutput[i]  ;
                double fractionaluse = usage /resource;
                if (fractionaluse > maxfrac) maxfrac=fractionaluse;
            }
        double expansionratio = capacitytarget/maxfrac;
        if(phase1rescale)
            // expand overall scale of production to balance
            for (int i=0; i<intense.length; i++)   intense[i]*=(expansionratio);
        // now make sure no other resource has a negative output
        netoutput=computeNetOutput(C,intense,initialresource);
        if(verbose) {
            writeln("post phase1");
            System.out.print("state,");
            write(netoutput);
            writeln(intense);


        }
        boolean allpositive = true;
        for(double d:netoutput)allpositive = allpositive && (d>=0);
        if (!allpositive)
            if(phase2rescale) {
                Vector< Vector<Integer> >ui=C. buildUserIndex();
                double [] grossAvail =computeGrossAvail(C,intense,initialresource);
                double [] shrinkby = new double[C.techniqueCount()];
                for(int i=0; i<shrinkby.length; i++)shrinkby[i]=1;
                for(int i=0; i<netoutput.length; i++)
                    if(netoutput[i]<0) {
                        double amountused = grossAvail[i]-netoutput[i];
                        double shortfallratio = capacitytarget*(grossAvail[i] )/amountused;
                        Vector<Integer>users = ui.elementAt(i);
                        double weight=0;
                        // go through all techniques which use product i
                        for(Integer I:users) { // big I is a technique number
                            Technique t= C.techniques.elementAt(I.intValue());
                            // check that they do not actually make product i as output
                            if(t.productCode!=i) {
                                // reduce its intensity by the shortfall ratio
                                if (shortfallratio<shrinkby[I])
                                    shrinkby[I]= shortfallratio;
                            }
                        }

                    }
                for(int i=0; i<shrinkby.length; i++)intense[i]*=shrinkby[i];
            }
        if(verbose) {
            writeln("postphase2");
            System.out.print("state,");
            write(netoutput);
            writeln(intense);


        }
    }
    static void initialiseIntensities(double[]intensity,TechnologyComplex C, double [] initialresource ) {
        for (int i=0; i<intensity.length; i++)
            intensity[i]=0.1 ;
        rescaleIntensity(intensity,C,initialresource);
    }
    static void equaliseHarmony(double [] intensity,
                                double [] derivativeOfProductHarmony,
                                double []netproduct,
                                double temperature,
                                TechnologyComplex C,
                                double[] h,
                                Vector< Vector<Integer>> index, double [] initialresource )throws IllegalIntensity {
        // find mean harmony
        double mh=mean(h,C);
        int k=0;
        for(k=0; k<h.length; k++)
            if(!C.nonproduced[k])
                if(!C.nonfinal[k]) {
                    // work out how much to change its output to get it on the mean
                    double excessH = ( h[k] -mh);
                    // divide this by the derivative to get change in output
                    double changeOutput = temperature*excessH/derivativeOfProductHarmony[k];
                    double fractionalchange = sigmoid (changeOutput/(netproduct[k]==0?1:netproduct[k]));

                    Vector<Integer> productionSet =index.elementAt(k);
                    for(Integer I:productionSet) {
                        // sign is negative since we reduce the high harmonies
                        intensity[I]*= (1-fractionalchange);
                        if(intensity[I]<0)throw new IllegalIntensity(" intensity "+I+" went negative, fractional change = "+fractionalchange);
                    }
                }
    }
    static double sigmoid(double d) {
        if (d>0) return d/(1+d);
        if (d==0) return 0;
        d= -d;
        return -(d/(1+d) );
    }

    static void adjustIntensities(double [] intensity,
                                  double [] derivativeOfProductHarmony,
                                  double temperature,
                                  TechnologyComplex C,
                                  double[] h,
                                  Vector< Vector<Integer>> index,
                                  double [] initialresource,
                                  double[] planTargets)throws IllegalIntensity
    {   double []netOutput;
        netOutput=computeNetOutput(C,intensity,initialresource);
        if(verbose) {
            writeln("preequalisation");
            printstate(  intensity,  C,  initialresource,   planTargets);
        }
        equaliseHarmony(  intensity,
                          derivativeOfProductHarmony,
                          netOutput,
                          temperature,
                          C,
                          h,
                          index,
                          initialresource );
        netOutput=computeNetOutput(C,intensity,initialresource);
        derivativeOfProductHarmony=computeHarmonyDerivatives(  netOutput,  planTargets,  C, intensity );
        if(verbose) {
            writeln("prereallocation");
            printstate(  intensity,  C,  initialresource,   planTargets);
        }
        double[] expansionrate=new double[C.techniques.size()];
        for(int i=0; i<C.techniques.size(); i++)
        {
            Technique t= C.techniques.elementAt(i);
            expansionrate[i] = t.rateOfHarmonyGain(derivativeOfProductHarmony);
        }
        double meane = mean(expansionrate);
        for(int i=0; i<C.techniques.size(); i++)
        {

            double adjustedexp=sigmoid( expansionrate[i] )*temperature*phase2adjust  ;
            // absolute limit to shrink rate
            // shrink or expand in proportion to gains
            intensity[i]*=(1+ adjustedexp);
            if(intensity[i]<0)throw new IllegalIntensity(" intensity "+i+" went negative, adjustedexp=" +adjustedexp);
        }
        netOutput=computeNetOutput(C,intensity,initialresource);
        derivativeOfProductHarmony=computeHarmonyDerivatives(  netOutput,  planTargets,  C, intensity );
        if(verbose) {
            writeln("postreallocation");
            printstate(  intensity,  C,  initialresource,   planTargets);
        }


        rescaleIntensity(intensity,C,initialresource);
    }
    static double[] computeNetOutput(TechnologyComplex C, double [] intensity,double[]initial) {
        double [] output =  computeGrossAvail(  C,  intensity, initial);

        for(int j=0; j<C.techniqueCount(); j++) {
            Technique t= C.techniques.elementAt(j);

            for(int k=0; k<t.inputCodes.length; k++) {
                output[t.inputCodes[k]]-= intensity[j]*t.inputUsage[k];
            }
            if (t instanceof JointProductionTechnique) {
                JointProductionTechnique J=(JointProductionTechnique)t;
                int[] codes= J.getCoproductionCodes();
                double[]Q=J.getCoproductionQuantities();
                for (int k2=0; k2<codes.length; k2++) {
                    output[codes[k2]]+= intensity[j]*Q[k2];
                }
            }
        }
        return output;
    }
    /** gives the vector of total amount produced or available in initial resource vector - does not deduct productive consumption */
    static double[] computeGrossAvail(TechnologyComplex C, double [] intensity,double[]initial) {
        double [] output = new double[C.productCount()];
        for(int i =0; i<output.length; i++)output[i]=initial[i];
        for(int j=0; j<C.techniqueCount(); j++) {
            Technique t= C.techniques.elementAt(j);
            output [t.productCode]+= t.grossOutput*intensity[j];

        }
        return output;
    }
    /** we test it using Kantorovich's excavator example */
    public static void main(String[] args) {
        int dest =0;
        int src=1;
        int products=6;
        // matrix format of the problem as used in the Pascal Kantorovich solver
        double [][][]ctechniques   = {
            {{105,0,0,0,0,0},{0,0,0, 1,0,0}},
            {{107,0,0,0,0,0},{0,0,0, 0,1,0}},
            {{64,0,0,0,0,0},{0,0,0, 0,0,1}},
            {{0,56,0,0,0,0},{0,0,0, 1,0,0}},
            {{0,66,0,0,0,0},{0,0,0, 0,1,0}},
            {{0,38,0,0,0,0},{0,0,0,  0,0,1}},
            {{0,0,56,0,0,0},{0,0,0, 1,0,0}},
            {{0,0,83, 0,0,0},{0,0,0,0,1,0}},
            {{0,0,53, 0,0,0},{0,0,0, 0,0,1}}
        };
        TechnologyComplex C= new TechnologyComplex(products);
        // name the goods
        String[] labels= {"A","B","C","M1","M2","M3"};
        for (int l=0; l<6; l++)C.setProductName(l,labels[l]);
        for (int i=0; i<ctechniques.length; i++) {
            int productCode=firstnonzero(ctechniques[i][dest]);
            int srcCode = firstnonzero(ctechniques[i][src]);
            double[] usage= {ctechniques[i][src][srcCode]};
            int[]codes= {srcCode};
            Technique t;
            C.addTechnique( t=new Technique("T"+i,productCode,ctechniques[i][dest][productCode],usage,codes));

        }
        // now add fixed techniques to supply initial resources
        double [] initialresource = {0,0,0,1,1,1};
        for (int j =3; j<6; j++) {
            C.nonfinal[j]=true;
            C.nonproduced[j]=true;
        }
        //    for (Technique t0:C.techniques)writeln(""+t0);
        // now set the plan target
        double[]ctarget  = {64,64,64,0.05,0.05,0.05};
        double[] kantorovichsanswer =
        {
            0.671365,      0,      0,
            0.328635,0.789238,      0,
            0,    0.210762,          1
        };

        try {
            double[] intensity=balancePlan(  C, ctarget,initialresource);
            double [] netoutput=computeNetOutput(C,intensity,initialresource);
            writeln("iters "+iters);
            writeln("phase 2 adjust "+phase2adjust +" starting temp "+startingtemp +"capacity target" +capacitytarget+" use weight "+useweight);
            writeln("net outputs");
            writeln( netoutput);

            writeln("our intensities, followed by Kantorovich's ones ");
            writeln(intensity);
            writeln(kantorovichsanswer);
        } catch(Exception e) {
            System.err.println("fail "+e);
            e.printStackTrace();
        }
    }
    static int firstnonzero(double[]d) {
        for(int i=0; i<d.length; i++)
            if(d[i]!=0.0)return i;
        return d.length;
    }
}
class InconsistentScale extends Exception {
    InconsistentScale(String s) {
        super(s);
    }
}
class IllegalIntensity extends Exception {
    IllegalIntensity(String s) {
        super(s);
    }
}
