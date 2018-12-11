unit harmony;
(*! A class to optimise a set of linear production technologies to meet
a Kantorovich style output target and having a pregiven set of initial resources.<p>
 
  It produces an output file of the plan in lp-solve format on standard out<p>
   Class to provide optimisation of plans using the algorithm in Towards a New Socialism
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
 *)
 interface
 uses technologies;
 function H( target,  netoutput:real) :real;
 
 function dH(target,  netoutput:real) :real;
 const
    useweight:real = 5;
    phase2adjust:real = 0.3 ;
    capacitytarget:real=0.98;
    startingtemp:real=0.23;
    meanh:real=0;
    phase1rescale:boolean=true;
    phase2rescale:boolean=true;
    iters:integer=80;
    verbose:boolean=false;
 
var
    productHarmony:pvec;
 
implementation
 
    (*! the derivative of the harmony function
     * evaluated numerically so as to be independent of the H function *)
function fdH(target,  netoutput:real) :real;
var epsilon, base, baseplusEpsilon:real;
begin

	epsilon := 0.000001;
	base := H(target,netoutput);
	basePlusEpsilon := H(target, epsilon+netoutput);
	fdh:= (basePlusEpsilon - base)/epsilon;
	 
end;

function H( target,  netoutput:real) :real;
 var scale:real
 begin
	 scale :=  (netoutput-target  )/target;
	 if (scale<0) then H:= scale - (scale*scale)*0.5
	 else H:= ln(scale+1);
 end;
 (*! Forward procedure declarations *)
 function meanv(var m:vector  ):real;forward;
 function computeHarmonyDerivatives(var netOutput, planTargets:vector;C:TechnologyComplex;var intentsity:vector):pvec ;forward;
 procedure printstateS(var netOutput,productHarmonyDerivatives,productHarmony:vector;var C:TechnologyComplex ;var intensity:vector);forward;
 function mean(var  m:vector; var C:TechnologyComplex   ):real;forward;
 function nonfinalHarmonyDerivativeMax(var netOutput:vector;  nonfinal:integer;var dharmonies:vector;var C:TechnologyComplex  ):real;
     
 function computeNetOutput(var C:TechnologyComplex ; var  intensity,initial:vector ):pvec;forward;
 procedure rescaleIntensity(var intense:vector;C:TechnologyComplex ; var initialresource:vector);forward;
 function sigmoid(  d:real):real;forward;
 procedure initialiseIntensities(var intensity:vector;var C:TechnologyComplex ;  var initialresource:vector ) ;forward;
 procedure  equaliseHarmony( var intensity,
                                  derivativeOfProductHarmony,
                                  netproduct:vector;
                                  temperature:real;
                                var C:TechnologyComplex ;
                                var h:vector;
                                var index:pdvec; var initialresource:vector );forward;
   (*! C is a technology complex, fixed resources should be added as nonproduced products<p>
      planTargets is the target output of each product<p>
      returns a vector of technology intensities*)
  function balancePlan(var C:TechnologyComplex ; var  planTargets, initialresource:vector ):pvec;
    label 99;
     
    var producerindex:pdvec;
        intensity,netoutput,productHarmonyDerivatives:pvec;
        t,meanh:real;
        i:integer;
       function  computeHarmony (var netOutput,  planTargets:vector;C:TechnologyComplex ;var  intentsity:vector ):pvec;
        var lh:pvec;i:integer;
        begin
            new(lh,netOutput.length);       
            lh^:=  H(planTargets ,netOutput );
            computeHarmony:=lh;
        end;
        procedure printstate(var intensity:vector;C:TechnologyComplex;var  initial,  targets:vector) ;
        var netoutput, h, hd:pvec;
        begin
	         netOutput := computeNetOutput(C,intensity,initial );
	         h :=computeHarmony(netOutput^,targets,C,intensity );
	         hd := computeHarmonyDerivatives(netOutput^,targets,C,intensity );
	         printstateS(netOutput^, hd^,h^,C,intensity);
	         dispose(h);dispose(hd);dispose(netOutput);
	    end;
        procedure adjustIntensities(var intensity:vector;
                                    var derivativeOfProductHarmony:pvec;
                                        temperature:real;
                                  var   C:technologycomplex;
                                  var   h:vector;
                                  var   index:pdvec;
                                  var   initialresource,
                                      planTargets:vector);
        var netOutput:pvec;  expansionrate:pvec;  techniques:ptechniquelist;   t:ptechnique; 
            meane,adjustedexp:real;  i,j:integer;        
	    begin    
	        netOutput:=computeNetOutput(C,intensity,initialresource);
	        if(verbose) then begin
	            writeln('preequalisation');
	            printstate(  intensity,  C,  initialresource,   planTargets);
	        end;
	        equaliseHarmony(  intensity,
	                          derivativeOfProductHarmony^,
	                          netOutput^,
	                          temperature,
	                          C,
	                          h,
	                          index,
	                          initialresource );
	        dispose(netOutput);
	        netOutput:=computeNetOutput(C,intensity,initialresource);
	        derivativeOfProductHarmony:=computeHarmonyDerivatives(  netOutput^,  planTargets,  C, intensity );
	        if(verbose) then begin
	            writeln('prereallocation');
	            printstate(  intensity,  C,  initialresource,   planTargets);
	        end;
	        new(expansionrate,  C.techniquecount);
	        techniques:=C.techniques;
	        for i:= 1 to C.techniquecount do
	        if techniques <>nil then
	        begin
	            t:= techniques^.tech;
	            expansionrate^[i] :=  rateOfHarmonyGain(t^,derivativeOfProductHarmony^);
	            techniques:=techniques^.next;
	        end;
	        meane := meanv(expansionrate^);
	        for i:= 1 to C.techniquecount do
	        begin
	
	            adjustedexp:=sigmoid( expansionrate^[i] )*temperature*phase2adjust  ;
	            // absolute limit to shrink rate
	            // shrink or expand in proportion to gains
	            intensity[i]:=intensity[i]*(1+ adjustedexp);
	            if(intensity[i]<0)then 
	            begin 
	               writeln(' intensity ',i,' went negative, adjustedexp=' ,adjustedexp);
	               goto 99;
	            end;
	        end;
	        dispose(netOutput);
	        netOutput:=computeNetOutput(C,intensity,initialresource);
	        dispose(derivativeOfProductHarmony);
	        derivativeOfProductHarmony:=computeHarmonyDerivatives(  netOutput^,  planTargets,  C, intensity );
	        if(verbose) then
	        begin
	            writeln('postreallocation');
	            printstate(  intensity,  C,  initialresource,   planTargets);
	        end;
	        rescaleIntensity(intensity,C,initialresource);
	    end;
 
    begin
        if(planTargets.length <> C.productCount)then
        begin
              writeln('plan target has length ',planTargets.cols,
              ' but the number of products in TechnologyComplex is ', C.techniqueCount     );
              balancePlan:=nil;
              goto 99;
        end;
        producerIndex:=C.buildProducerIndex;
        new(intensity, C.techniqueCount);     
        initialiseIntensities(intensity^,C,initialresource);
        t:=startingtemp;
        new ( productHarmony, C.productCount);     
        for i:=0 to iters-1 do   begin
            netOutput := computeNetOutput(C,intensity^,initialresource);
            productHarmony^:=  H(planTargets ,netOutput^);
            meanh:=mean(productHarmony^,C);
            productHarmonyDerivatives := computeHarmonyDerivatives(netOutput^,planTargets,C,intensity^ );
            adjustIntensities(intensity^,
                              productHarmonyDerivatives,
                              t,
                              C,
                              productHarmony^,
                              producerIndex,
                              initialresource,planTargets);
            if(verbose) then printstate(  intensity^,  C,  initialresource,   planTargets);
            dispose(netOutput);
            dispose(productHarmonyDerivatives);
        end;
        balancePlan:= intensity;
        99:
    end;
    (*! compute the derivatives of the harmonies of all products with repect to marginal increase in output in terms of
    actual output units not intensities *)
    function computeHarmonyDerivatives(var netOutput, planTargets:vector;C:TechnologyComplex;var intentsity:vector):pvec ;
    var dh:pvec;i,solve:integer;
    begin
        new(dh,  netOutput.cols);
        
        dh^ :=  fdH(planTargets ,netOutput );
       
        for solve := 1 to 2 do        
           for i:=1 to dh^.cols do
                if(C.nonfinal^[i])then
                begin{ weighted average of derivative due to shortage and due to potential other use}
                    dh^[i]:= (dh^[i]+ useweight* nonfinalHarmonyDerivativeMax(netOutput,i,dh^,  C )   )/(useweight+1);
                end;
        computeHarmonyDerivatives:= dh;
    end;
  
    procedure printstateS(var netOutput,productHarmonyDerivatives,productHarmony:vector;var C:TechnologyComplex ;var intensity:vector);
    var expansionrate:pvec;i:integer;t:ptechniquelist;
     begin
        write('netoutput or intensity,');
        write(netOutput);
        writeln(intensity);
        write('h ,');
        writeln(productHarmony);

        write('dh/dp or gainrate,');
        write (productHarmonyDerivatives);
        new( expansionrate,C.techniquecount);
        t:= C.techniques;
        for i:=1 to C.techniquecount do 
        if t<> nil then
        begin            
            expansionrate^[i] :=1+sigmoid( rateOfHarmonyGain(t^.tech^,productHarmonyDerivatives)) *startingtemp*phase2adjust ;
            t:= t^.next;
        end;
        writeln(expansionrate^);
        dispose(expansionrate);
    end;
   
    (*! for non final goods we make derivatives their harmonies the maximum of the derivatives of the harmonies of their users *)
    function nonfinalHarmonyDerivativeMax(var netOutput:vector;  nonfinal:integer;var dharmonies:vector;var C:TechnologyComplex  ):real;
    var max,total,d:real; best:integer; userIndex:pdvec; users:ptvec; i,techno:integer;t:technique;
        mpp:pvec;codes:pintvec;
        pt:ptechniquelist;
    begin
        
        userIndex:=C.buildUserIndex;
        
        max:= -1e22;
        total:=0;
        d:=0;
        best:=0;
        users := userIndex^[nonfinal];
        pt:=C.techniques;
        for i:=1 to users^.maxt do
        begin          
            t:= pt^.tech^;pt:=pt^.next;
            mpp:= marginalphysicalcoproducts(t, nonfinal);
            codes:= getCoproductionCodes(t);    
            d:=\+ dharmonies[codes]*mpp^; 
           
            dispose(mpp);dispose(codes);
            total := total +d;
            if((d)>max) then
                max:=d;
        end;
         nonfinalHarmonyDerivativeMax:= total/users^.maxt;
     
    end;
    
    function mean(var  m:vector;var C:TechnologyComplex  ):real;
    var sum :real; num,i:integer;
     begin
        sum := 0;
        num := 0;
        for i:=1 to m.cols do      
            if(not C.nonproduced^[i])then 
            begin
                sum := sum + m[i];
                num:= num+1;
            end;
        mean:= sum / num;
    end;
    function sdev(var  m:vector;var C:TechnologyComplex  ):real;
    var sum ,av:real; num,i:integer;
     begin
        sum := 0;av:=mean(m,C);
        num := 0;
        for i:=1 to m.cols do      
            if(not C.nonproduced^[i])then 
            begin  
                sum:= sum + (m[i]-av)*(m[i]-av) ;
                num:= num +1;
            end;
        sdev:=  sqrt(sum / num);
    end;
    function meanv(var m:vector  ):real;
     var sum :real; num,i:integer;
     begin
        sum := 0;
        num := 0;
        for i:=1 to m.cols do      
         
            begin
                sum := sum + m[i];
                num:= num+1;
            end;
        meanv:= sum / num;
    end;
    function  stdev(var m:vector;  av:real) :real;   
    var sum ,av:real; num,i:integer;
     begin
        sum := 0;av:=meanv(m);
        num := 0;
        for i:=1 to m.cols do             
            begin  
                sum:= sum + (m[i]-av)*(m[i]-av) ;
                num:= num +1;
            end;
        stdev:=  sqrt(sum / num);
    end;
    (*! gives the vector of total amount produced or available in initial resource vector - does not deduct productive consumption *)
    function computeGrossAvail(var C:TechnologyComplex ;var    intensity, initial:vector):pvec;
     var output:pvec;j,i,p:integer;ltrav:ptechniquelist;t:technique;
     begin
        new( output ,C.productCount);
        output^:=  initial;ltrav:= C.techniques;
        for j:=1 to C.techniqueCount do
        if ltrav <> nil then
        begin
            t:= ltrav^.tech^; 
            for i:= 1 to t.produces^.max do
            begin
				p:=t.produces^[i].product^.productNumber;
                output^[p]:= output^[p]+t.produces^[i].quantity*intensity[j];
			end;
			ltrav:= ltrav^.next;
        end;
       computeGrossAvail:=output;
    end;
    (*! shrink or expand all industries in order to not exceed target level of use of the critical fixed reource *)
    
    procedure rescaleIntensity(var intense:vector;var C:TechnologyComplex ; var initialresource:vector);
    var netoutput:pvec;maxfrac,resource,usage,fractionaluse,expansionratio:real;i:integer;
      grossAvail, shrinkby:pvec;
      allpositive:boolean;ui:pdvec;
     begin
         netoutput:=computeNetOutput(C,intense,initialresource);
        maxfrac:=0;
       for i:=1 to netoutput^.cols do 
        if(C.nonproduced^[i])then
        begin
                 resource := initialresource[i];
                 usage :=resource- netoutput^[i]  ;
                 fractionaluse := usage /resource;
                 if (fractionaluse > maxfrac) then maxfrac:=fractionaluse;
         end;
        expansionratio := capacitytarget/maxfrac;
        if(phase1rescale)then
            // expand overall scale of production to balance
            intense:= expansionratio;
        dispose(netoutput);   
        // now make sure no other resource has a negative output
        netoutput:=computeNetOutput(C,intense,initialresource);
        if(verbose)then 
        begin
            writeln('post phase1');
            write('state,');
            write(netoutput^);
            writeln(intense);
        end;
        allpositive:= \and( (netoutput^)>=0);
        
        if (not allpositive) then
            if(phase2rescale)then
            begin
                ui:=C. buildUserIndex;
                grossAvail :=computeGrossAvail(C,intense,initialresource);
                new(shrinkby, C.techniqueCount);
                shrinkby^:=1;
                for i:= 1 to netoutput^.cols do 
                    if(netoutput^[i]<0)then begin
                        double amountused = grossAvail[i]-netoutput[i];
                        double shortfallratio = capacitytarget*(grossAvail[i] )/amountused;
                        Vector<Integer>users = ui.elementAt(i);
                        double weight=0;
                        // go through all techniques which use product i
                        for(Integer I:users) begin // big I is a technique number
                            Technique t= C.techniques.elementAt(I.intValue());
                            // check that they do not actually make product i as output
                            if(t.productCode!=i) begin
                                // reduce its intensity by the shortfall ratio
                                if (shortfallratio<shrinkby[I])
                                    shrinkby[I]= shortfallratio;
                            end
                        end

                    end
                for(int i=0; i<shrinkby.length; i++)intense[i]*=shrinkby[i];
            end
        if(verbose) begin
            writeln('postphase2');
            System.out.print('state,');
            write(netoutput);
            writeln(intense);


        end;
        dispose(netoutput);
    end
    procedure initialiseIntensities(var intensity:vector;C:TechnologyComplex ;var initialresource :vector);
    var i:integer; 
    begin
        intensity:=0.1;
        rescaleIntensity(intensity,C,initialresource);
    end;
    procedure  equaliseHarmony( var intensity,
                                   derivativeOfProductHarmony,
                                   netproduct:vector;
                                  temperature:real;
                                var C:TechnologyComplex ;
                                var h:vector;
                                var index:pdvec; var initialresource:vector );
        begin
        // find mean harmony
        double mh=mean(h,C);
        int k=0;
        for(k=0; k<h.length; k++)
            if(!C.nonproduced[k])
                if(!C.nonfinal[k]) begin
                    // work out how much to change its output to get it on the mean
                    double excessH = ( h[k] -mh);
                    // divide this by the derivative to get change in output
                    double changeOutput = temperature*excessH/derivativeOfProductHarmony[k];
                    double fractionalchange = sigmoid (changeOutput/(netproduct[k]==0?1:netproduct[k]));

                    Vector<Integer> productionSet =index.elementAt(k);
                    for(Integer I:productionSet) begin
                        // sign is negative since we reduce the high harmonies
                        intensity[I]*= (1-fractionalchange);
                        if(intensity[I]<0)throw new IllegalIntensity(' intensity '+I+' went negative, fractional change = '+fractionalchange);
                    end
                end
    end
    function sigmoid(  d:real):real;
     begin
        if (d>0) return d/(1+d);
        if (d==0) return 0;
        d= -d;
        return -(d/(1+d) );
    end

       static double[] computeNetOutput(TechnologyComplex C, double [] intensity,double[]initial) begin
        double [] output =  computeGrossAvail(  C,  intensity, initial);

        for(int j=0; j<C.techniqueCount(); j++) begin
            Technique t= C.techniques.elementAt(j);

            for(int k=0; k<t.inputCodes.length; k++) begin
                output[t.inputCodes[k]]-= intensity[j]*t.inputUsage[k];
            end
            if (t instanceof JointProductionTechnique) begin
                JointProductionTechnique J=(JointProductionTechnique)t;
                int[] codes= J.getCoproductionCodes();
                double[]Q=J.getCoproductionQuantities();
                for (int k2=0; k2<codes.length; k2++) begin
                    output[codes[k2]]+= intensity[j]*Q[k2];
                end
            end
        end
        return output;
    end
    
    (*! we test it using Kantorovich's excavator example *)
    public static void main(String[] args) begin
        int dest =0;
        int src=1;
        int products=6;
        // matrix format of the problem as used in the Pascal Kantorovich solver
        double [][][]ctechniques   = begin
            ((105,0,0,0,0,0),(0,0,0, 1,0,0)),
            ((107,0,0,0,0,0),(0,0,0, 0,1,0)),
            ((64,0,0,0,0,0),(0,0,0, 0,0,1)),
            ((0,56,0,0,0,0),(0,0,0, 1,0,0)),
            ((0,66,0,0,0,0),(0,0,0, 0,1,0)),
            ((0,38,0,0,0,0),(0,0,0,  0,0,1)),
            ((0,0,56,0,0,0),(0,0,0, 1,0,0)),
            ((0,0,83, 0,0,0),(0,0,0,0,1,0)),
            ((0,0,53, 0,0,0),(0,0,0, 0,0,1))
        end;
        TechnologyComplex C= new TechnologyComplex(products);
        // name the goods
        String[] labels= begin'A','B','C','M1','M2','M3'end;
        for (int l=0; l<6; l++)C.setProductName(l,labels[l]);
        for (int i=0; i<ctechniques.length; i++) begin
            int productCode=firstnonzero(ctechniques[i][dest]);
            int srcCode = firstnonzero(ctechniques[i][src]);
            double[] usage= beginctechniques[i][src][srcCode]end;
            int[]codes= beginsrcCodeend;
            Technique t;
            C.addTechnique( t=new Technique('T'+i,productCode,ctechniques[i][dest][productCode],usage,codes));

        end
        // now add fixed techniques to supply initial resources
        double [] initialresource = begin0,0,0,1,1,1end;
        for (int j =3; j<6; j++) begin
            C.nonfinal[j]=true;
            C.nonproduced[j]=true;
        end
        //    for (Technique t0:C.techniques)writeln(''+t0);
        // now set the plan target
        double[]ctarget  = begin64,64,64,0.05,0.05,0.05end;
        double[] kantorovichsanswer =
        begin
            0.671365,      0,      0,
            0.328635,0.789238,      0,
            0,    0.210762,          1
        end;

        try begin
            double[] intensity=balancePlan(  C, ctarget,initialresource);
            double [] netoutput=computeNetOutput(C,intensity,initialresource);
            writeln('iters '+iters);
            writeln('phase 2 adjust '+phase2adjust +' starting temp '+startingtemp +'capacity target' +capacitytarget+' use weight '+useweight);
            writeln('net outputs');
            writeln( netoutput);

            writeln('our intensities, followed by Kantorovich's ones ');
            writeln(intensity);
            writeln(kantorovichsanswer);
        end catch(Exception e) begin
            System.err.println('fail '+e);
            e.printStackTrace();
        end
    end
    static int firstnonzero(double[]d) begin
        for(int i=0; i<d.length; i++)
            if(d[i]!=0.0)return i;
        return d.length;
    end
end
 begin
 end.
