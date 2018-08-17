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

    static final double useweight = 4;
    static final double phase2adjust = 0.375;
    static final double capacitytarget=0.97;
    static final double startingtemp=0.85;
    static double meanh=0;
    static int iters=1500;
    static double [] productHarmony= {};
    /** C is a technology complex, fixed resources should be added as nonproduced products

     * planTargets is the target output of each product<p>
     * returns a vector of technology intensities*/
    public static double [] balancePlan(TechnologyComplex C, double[] planTargets, double [] initialresource )throws InconsistentScale {
        if(planTargets.length != C.productCount())
            throw new InconsistentScale(
                "plan target has length "+planTargets.length+" but the number of products in TechnologyComplex is "+C.techniqueCount()
            );
        Vector< Vector<Integer>> producerIndex=C.buildProducerIndex();
        double [] intensity = new double[C.techniqueCount()];
        initialiseIntensities(intensity,C ,initialresource);
        double t=startingtemp;
        productHarmony=new double[C.productCount()];
        for(int i=0; i<iters; i++) {
            //   writeln("\niteration "+i+" temp "+t);
            double [] netOutput = computeNetOutput(C,intensity,initialresource);

            for(int k=0; k<netOutput.length; k++)
                if(!C.nonfinal[k])productHarmony[k]=   ( Harmony.H(planTargets[k],netOutput[k]));
            meanh=mean(productHarmony,C);
            //  writeln("meanh "+meanh);

            double [] productHarmonyDerivatives = computeHarmonyDerivatives(netOutput,planTargets,C,intensity,useweight);

            adjustIntensities(intensity,
                              productHarmonyDerivatives,
                              t,
                              C,
                              productHarmony,
                              producerIndex,
                              initialresource,planTargets);

        }
        writeln(productHarmony);
        writeln("meanh "+meanh);
        return intensity;
    }/** compute the derivatives of the harmonies of all products with repect to marginal increase in output in terms of
    actual output units not intensities */
    static  double []  computeHarmonyDerivatives(double[] netOutput,double[] planTargets,TechnologyComplex C,double[] intentsity,double usageweight) {
        double []dh=new double[netOutput.length];
        for (int i=0; i<dh.length; i++) {
            dh[i]= Harmony.dH(planTargets[i],netOutput[i]);
        }
        for (int i=0; i<dh.length; i++)
            if(C.nonfinal[i]) {// weighted average of derivative due to shortage and due to potential other use
                dh[i]= (dh[i]+ usageweight* nonfinalHarmonyDerivativeMax(netOutput,i,dh,  C )   )/(usageweight+1);
            }
        return dh;
    }
    static void  writeln(String s) {
        System.out.println(s);
    }
    static void  writeln(double []d) {
        for(int i=0; i<d.length; i++)System.out.printf("%5.4f,",d[i]);
        writeln("");
    }
    /** for non final goods we make derivatives their harmonies the maximum of the derivatives of the harmonies of their users */
    static double nonfinalHarmonyDerivativeMax(double[] netOutput,int input,double [] dharmonies,TechnologyComplex C ) {
        Vector< Vector<Integer>> userIndex;
        userIndex=C.buildUserIndex();
        double max;
        max= -1e22;
        int best;
        best=0;
        Vector<Integer> users = userIndex.elementAt(input);
        for(int i=0; i<users.size(); i++) {
            int techno=users.elementAt(i);
            Technique t= C.techniques.elementAt(techno);
            int produces =t.getProductCode();
            double dhp= dharmonies[produces];
            double d= dhp*marginalphysicalproduct(  techno,   input, C );
    
            if((d)>max) {
                max=d;
                best=t.getProductCode();
            }
           
        }
        
        return max;
    }
    /** marginal physical product of technology techno with respect to the input */
    static double marginalphysicalproduct(int techno, int input, TechnologyComplex C ) {

        Technique user=C.techniques.elementAt(techno);

        int pos =findiIna(input, user.inputCodes);

        return  user.grossOutput/ user.inputUsage[pos] ;// the more input needed the less it contributes


    }
    static int findiIna(int i , int[] a) {
        for (int j=0; j<a.length; j++)if(a[j]==i)return j;
        return -1;
    }
    /** compute the derivative of total harmony with respect to the intensity of a technique */
    static double[]computeTechniqueHarmonyDerivatives(TechnologyComplex C,double []productHarmonyDerivatives ) {
        double [] thd = new double [ C.techniqueCount()];
        for (int i=0; i<C.techniqueCount(); i++) {
            Technique t= C.techniques.elementAt(i);
            thd[i]=t.grossOutput*productHarmonyDerivatives [t.productCode] ;
            for(int k=0; k<t.inputCodes.length; k++) {
                thd[i]-= t.inputUsage[k]*productHarmonyDerivatives[t.inputCodes[k]] ;
            }
        }
        return thd;
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
    static double sdev(double[] m , TechnologyComplex C) {
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
    static double sdev(double[] m ,double av) {
        double sum = 0;
        int num=0;
        for (int i = 0; i < m.length; i++) {
            sum += (m[i]-av)*(m[i]-av) ;
            num++;
        }
        return Math.sqrt(sum / num);
    }
    static void rescaleIntensity(double[]intense,TechnologyComplex C , double [] initialresource) {
        double [] netoutput=computeNetOutput(C,intense,initialresource);
        double maxfrac=0;
        for (int i=0; i<netoutput.length; i++) if(C.nonproduced[i]) {
                double resource = initialresource[i];
                double usage =resource- netoutput[i]  ;
                double fractionaluse = usage /resource;

                if (fractionaluse > maxfrac) maxfrac=fractionaluse;
            }
        double expansionratio = capacitytarget/maxfrac;

        // expand overall scale of production to balance
        for (int i=0; i<intense.length; i++)   intense[i]*=(expansionratio);

    }
    static void initialiseIntensities(double[]intensity,TechnologyComplex C , double [] initialresource ) {
        for (int i=0; i<intensity.length; i++)
            intensity[i]=0.1 ;
        rescaleIntensity(intensity ,C,initialresource);
    }
    static void equaliseHarmony(double [] intensity,
                                double [] derivativeOfProductHarmony,
                                double []netproduct,
                                double temperature,
                                TechnologyComplex C,
                                double[] h,
                                Vector< Vector<Integer>> index, double [] initialresource ) {
        // find mean harmony
        double mh=mean(h,C);
        int k=0;
        for(k=0; k<h.length; k++)
            if(!C.nonproduced[k]) {
                // work out how much to change its output to get it on the mean
                double excessH = h[k] -mh;
                // divide this by the derivative to get change in output
                double changeOutput = temperature*excessH/derivativeOfProductHarmony[k];
                double fractionalchange = changeOutput/netproduct[k];
                Vector<Integer> productionSet =index.elementAt(k);
                for(Integer I:productionSet) {
                    // sign is negative since we reduce the high harmonies
                    intensity[I]*= (1-fractionalchange);
                }
            }
    }
    static void adjustIntensities(double [] intensity,
                                  double [] derivativeOfProductHarmony,
                                  double temperature,
                                  TechnologyComplex C,
                                  double[] h,
                                  Vector< Vector<Integer>> index,
                                  double [] initialresource ,
                                  double[] planTargets)
    {
        equaliseHarmony(  intensity,
                          derivativeOfProductHarmony,
                          computeNetOutput(C,intensity,initialresource),
                          temperature,
                          C,
                          h,
                          index,
                          initialresource );

        for(int i=0; i<C.techniques.size(); i++)
        {
            Technique t= C.techniques.elementAt(i);
            double gain = derivativeOfProductHarmony[t.productCode]*t.grossOutput;
            double cost=0;
            for(int j=0; j<t.inputCodes.length; j++) {
                cost+=derivativeOfProductHarmony[t.inputCodes[j]]*t.inputUsage[j];
            }
            // shrink or expand in proportion to gains
            intensity[i]*= (1+(gain-cost)*temperature*phase2adjust/cost);
        }
        rescaleIntensity(intensity ,C,initialresource);
    }



    static double[] computeNetOutput(TechnologyComplex C, double [] intensity,double[]initial) {
        double [] output = new double[C.productCount()];
        for(int i =0; i<output.length; i++)output[i]=initial[i];

        for(int j=0; j<C.techniqueCount(); j++) {
            Technique t= C.techniques.elementAt(j);
            output [t.productCode]+= t.grossOutput*intensity[j];
            for(int k=0; k<t.inputCodes.length; k++) {
                output[t.inputCodes[k]]-= intensity[j]*t.inputUsage[k];
            }
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
        double[]ctarget  = {64 ,64 ,64 ,0.05,0.05,0.05};
       double[] kantorovichsanswer =
{
    0.671365     ,      0     ,      0,
    0.328635    ,0.789238     ,      0,
           0,    0.210762 ,          1};
 
        try {
            double[] intensity=balancePlan(  C, ctarget,initialresource);
            double [] netoutput=computeNetOutput(C,intensity,initialresource);
            writeln("iters "+iters);
            writeln("phase 2 adjust "+phase2adjust +" starting temp "+startingtemp +"capacity target" +capacitytarget+" use weight "+useweight);
            writeln("net outputs");
            writeln( netoutput);
            
            writeln("our intensities, followed by Kantorovich's ones ");writeln(intensity);
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
