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
    phase2adjust:real = 0.4 ;
    capacitytarget:real=0.98;
    startingtemp:real=0.23;
    meanh:real=0;
    phase1rescale:boolean=true;
    phase2rescale:boolean=true;
    iters:integer=100;
    verbose =false;
  
 function balancePlan(  var  planTargets, initialresource:vector;var C:technologycomplex ):pvec;
   
 procedure printstateS(var netOutput,productHarmonyDerivatives,productHarmony:vector;
                       var C:TechnologyComplex ;
                       var intensity:vector);
 function mean(var  m:vector; var C:TechnologyComplex   ):real;
 function nonfinalHarmonyDerivativeMax(var netOutput:vector;  nonfinal:integer;var dharmonies:vector;var C:TechnologyComplex  ):real;
 function computeGrossAvail(var C:TechnologyComplex ;var    intensity, initial:vector):pvec;   
 function computeNetOutput(var C:TechnologyComplex ; var  intensity,initial:vector ):pvec;
 procedure rescaleIntensity(var intense:vector;var C:TechnologyComplex ; var initialresource:vector);
 function sigmoid(  d:real):real;
 procedure initialiseIntensities(var intensity:vector;C:TechnologyComplex ;var initialresource :vector);
 procedure  equaliseHarmony( var intensity,
                                 derivativeOfProductHarmony,
                                  netproduct:vector;
                                  temperature:real;
                                var C:TechnologyComplex ;
                                var h:vector;
                                var index:pdvec; var initialresource:vector );
              
var
    productHarmony:pvec;
 
implementation
procedure rescaleIntensity(var intense:vector;var C:TechnologyComplex ; var initialresource:vector);
    var netoutput:pvec;
      amountused,shortfallratio,maxfrac,resource,usage,fractionaluse,expansionratio,weight:real;
      i,j:integer;
      grossAvail, shrinkby:pvec;
      users,pt:ptvec;
      t:ptechnique;
      allpositive:boolean;
      ui:pdvec;
     begin
       netoutput:=computeNetOutput(C,intense,initialresource);
       if(verbose)then 
        begin
            writeln('post phase0');
            writeln('netoutput');
            writeln(netoutput^);
            writeln('intensity');writeln(intense);
        end;
       maxfrac:=0;
       for i:=1 to C.nonproduced^.max   do 
        if(C.nonproduced^[i])then
        begin
                 resource := initialresource[i];
                 usage :=resource- netoutput^[i]  ;
                 fractionaluse := usage /resource;
              
                 if (fractionaluse > maxfrac) then maxfrac:=fractionaluse;
         end;
        expansionratio := capacitytarget/maxfrac;
          if(phase1rescale)then
           (*! expand overall scale of production to balance*)
            intense:= expansionratio*intense;
       
        dispose(netoutput);   
        (*! now make sure no other resource has a negative output*)
        netoutput:=computeNetOutput(C,intense,initialresource);
        if(verbose)then 
        begin
            writeln('post phase1');
            writeln('netoutput');
            writeln(netoutput^);
            writeln('intensity');
            writeln(intense);
        end;
        allpositive:= \and( (netoutput^)>=0);
        
        if (not allpositive) then
            if(phase2rescale)then
            begin
                ui:=  buildUserIndex(C);
                grossAvail :=computeGrossAvail(C,intense,initialresource);
                new(shrinkby, C.techniqueCount);
                shrinkby^:=1;
                for i:= 1 to netoutput^.cols do 
                    if(netoutput^[i]<0)then begin
                        amountused := grossAvail^[i]-netoutput^[i];
                        shortfallratio := capacitytarget*(grossAvail^[i] )/amountused;
                        users := ui^[i];
                        (*! Users is now a vector of all techqniques that use product i *)
                        weight:=0;
                        pt:=  techniques(C);
                         
                       (*! go through all techniques which use product i *)
                        for j:=1 to users^.maxt do
                        begin  
                            t:= users^[j];
                           if verbose then writeln(' for product ',i, 'user ',j, 'is technique number ',t^.techniqueno);
                            (*! check that they do not actually make product i as output *)
                            if not  produces(C,t^,i) then
                            begin
                                (*!reduce its intensity by the shortfall ratio *)
                                if (shortfallratio<shrinkby^[T^.Techniqueno])then
                                    shrinkby^[T^.Techniqueno]:= shortfallratio;
                            end;
                        end;

                    end;
                intense:= intense * shrinkby^;
                if(verbose)then 
		        begin
		            writeln('postphase2');
		            writeln('netoutput');
		            writeln(netoutput^); 
		            writeln('shrinkby');
		            writeln(shrinkby^);
		            writeln('intensity');
		            writeln(intense);
		        end;
                dispose(grossavail); 
                dispose(shrinkby);              
            end;
       
   
        dispose(netoutput);
    
        
end;
  
 
    (*! the derivative of the harmony function
     * evaluated numerically so as to be independent of the H function *)
function fdH(target,  netoutput:real) :real;
var epsilon, base, baseplusEpsilon:real;
begin

	epsilon := 0.0004;
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
                     

(*! gives the vector of total amount produced or available in initial resource vector - does not deduct productive consumption *)
    function computeGrossAvail(var C:TechnologyComplex ;var    intensity, initial:vector):pvec;
     var outputv:pvec;j,i,p:integer;ltrav:ptvec;t:ptechnique;f:real;
     begin
        new( outputv ,C.productCount);
        outputv^:=  initial;ltrav:=  techniques(C);
        for j:=1 to C.techniqueCount do
        
        begin
            t:= ltrav^[j]; 
            
            for i:= 1 to t^.produces^.max do
            begin
				p:=t^.produces^[i].product^.productNumber;
				f:=t^.produces^[i].quantity;
                outputv^[p]:= outputv^[p]+f*intensity[t^.techniqueno];
                
                if verbose then  
                begin
                writeln(t^.techniqueno,p,f,intensity[t^.techniqueno]);
                end; 
			end;
			 
        end;
       computeGrossAvail:=outputv;
    end;



   (*! C is a technology complex, fixed resources should be added as nonproduced products<p>
      planTargets is the target output of each product<p>
      returns a vector of technology intensities*)
  function balancePlan(  var  planTargets, initialresource:vector ;var C:technologycomplex):pvec;
    label 99;
 
  
    var producerindex:pdvec;
        intensity,netoutput,productHarmonyDerivatives:pvec;
        t,meanh:real;
        i:integer;
       function  computeHarmony (var netOutput,  planTargets:vector;C:TechnologyComplex ;var  intentsity:vector ):pvec;
        var lh:pvec;i:integer;
        begin
            new(lh,netOutput.cols);   
           
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
        var netOutput:pvec;  expansionrate:pvec;  ltechniques:ptvec;   t:ptechnique; 
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
	                          
	    //    dispose(netOutput);
	        netOutput:=computeNetOutput(C,intensity,initialresource);
	        derivativeOfProductHarmony:=computeHarmonyDerivatives(  netOutput^,  planTargets,  C, intensity );
	        if(verbose) then 
	        begin
	            writeln('prereallocation');
	            printstate(  intensity,  C,  initialresource,   planTargets);
	        end;
	        new(expansionrate,  C.techniquecount);
	        ltechniques:= techniques(C);
	        for i:= 1 to C.techniquecount do 
	        begin
	            t:= ltechniques^[i];
	            expansionrate^[t^.techniqueno] :=  rateOfHarmonyGain(t^,derivativeOfProductHarmony^);
	            
	        end;
	        meane := meanv(expansionrate^);
	        if verbose then 
	        begin write('expansionrate');writeln(expansionrate^); end;
	        for i:= 1 to C.techniquecount do
	        begin
	
	            adjustedexp:=sigmoid( expansionrate^[i] )*temperature*phase2adjust  ;
	           (*! absolute limit to shrink rate
	             shrink or expand in proportion to gains *)
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
        if verbose then begin
         writeln('balancePlan');writeln(planTargets, initialresource);
        end;
        if(planTargets.cols <> C.productCount)then
        begin
              writeln('plan target has length ',planTargets.cols,
              ' but the number of products in TechnologyComplex is ', C.productCount     );
              balancePlan:=nil;
              goto 99;
        end;
        producerIndex:= buildProducerIndex(C);
        new(intensity, C.techniqueCount);  
        initialiseIntensities(intensity^,C,initialresource);
        if(verbose)then begin 
			write('initialised intensity');
			writeln(intensity^);
		end;
        t:=startingtemp;
        new ( productHarmony, C.productCount);     
        for i:=0 to iters-1 do   
        begin 
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
        {$par}   
           for i:=1 to C.nonfinal^.max do
                if(C.nonfinal^[i])then
                begin{ weighted average of derivative due to shortage and due to potential other use}
                    dh^[i]:= (dh^[i]+ useweight* nonfinalHarmonyDerivativeMax(netOutput,i,dh^,  C )   )/(useweight+1);
                end;
        computeHarmonyDerivatives:= dh;
    end;
  
    procedure printstateS(var netOutput,productHarmonyDerivatives,productHarmony:vector;var C:TechnologyComplex ;var intensity:vector);
    var expansionrate,gainrate:pvec;i,pn:integer;t:ptvec;
     begin
        writeln('netoutput  ');
        write(netOutput);
        writeln('intensity');
        writeln(intensity);
        writeln('h ,');
        writeln(productHarmony);

        writeln('productHarmonyDerivatives');
        writeln (productHarmonyDerivatives);
        new( expansionrate,C.techniquecount);
        new(gainrate,C.techniquecount);
        t:=  techniques(C);
        for i:=1 to C.techniquecount do  
        begin    
            pn:=t^[i]^.techniqueno; 
            gainrate^[pn]:=  rateOfHarmonyGain(t^[i]^,productHarmonyDerivatives);     
            expansionrate^[pn] :=1+sigmoid( gainrate^[pn]) *startingtemp*phase2adjust ;       
        end;
        write ('gainrates, ');
        writeln(gainrate^);
        write ('expansionrates,');
        writeln(expansionrate^);
        dispose(expansionrate);dispose(gainrate);
    end;
   
    (*! for non final goods we make derivatives their harmonies the maximum of the derivatives of the harmonies of their users *)
    function nonfinalHarmonyDerivativeMax(var netOutput:vector;  nonfinal:integer;var dharmonies:vector;var C:TechnologyComplex  ):real;
    var max,total,d:real; best:integer; userIndex:pdvec; users:ptvec; i,techno:integer;t:ptechnique;
        mpp:pvec;codes:pintvec;
        pt:ptvec;
    begin        
        userIndex:= buildUserIndex(C);         
        max:= -1e22;
        total:=0;
        d:=0;
        best:=0;
        users := userIndex^[nonfinal];   
        if users=nil then 
        begin
			write('userIndex^[',nonfinal,'] is nil');halt(405);
        end;         
        pt:= techniques(C);
        for i:=1 to users^.maxt do
        begin                 
            t:= users^[i];        
            mpp:= marginalphysicalcoproducts(t^, C.allresourceindex^[nonfinal]);
            codes:= getCoproductionCodes(t^);         
            d:=\+ dharmonies[codes^]*mpp^;     
            dispose(mpp);
            dispose(codes);
            total := total +d;       
            if((d)>max) then
                max:=d;
        end;
         {nonfinalHarmonyDerivativeMax:= total/users^.maxt;}
		nonfinalHarmonyDerivativeMax:= max;
    end;
    
    function mean(var  m:vector;var C:TechnologyComplex  ):real;
    var sum :real; num,i:integer;
     begin
        sum := 0;
        num := 0;
        for i:=1 to C.nonproduced^.max do      
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
        for i:=1 to C.nonproduced^.max do      
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
    
    (*! shrink or expand all industries in order to not exceed target level of use of the critical fixed reource *)
    
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
                                var index:pdvec; 
                                var initialresource:vector );
        var mh,divisor:real; excessh,changeoutput,fractionalchange:real;k,j,i:integer;productionset:ptvec;
        begin        
      
        mh:=mean(h,C);
        for k:=1 to h.cols do        
            if(not C.nonproduced^[k])then
                if(not C.nonfinal^[k])then begin                  
                   (*!work out how much to change its output to get it on the mean *)
                    excessH := ( h[k] -mh);                     
                    (*!  divide this by the derivative to get change in output *)
                    changeOutput := temperature*excessH;                   
                    if netproduct[k]=0.0 then divisor:= 1.0 else divisor:=netproduct[k];                   
                    if derivativeofproductharmony[k]<>0 then
                    fractionalchange := sigmoid (changeOutput/(divisor*derivativeOfProductHarmony[k]))
                    else begin
						writeln('error, zero harmony derivative for product ',k);halt(406);
					end; 
                    productionSet :=index^[k];
                    if productionset=nil then
                    begin
						writeln('corrupt index in equalise harmony');halt(402);
                    end
                    else
                    for i:= 1 to productionset^.maxt do begin                      
                        if(productionset^[i] = nil)then begin 
							writeln('productionset[',i,'] is nil in equalise harmony');halt(404);
                        end;
                        j:=productionset^[i]^.techniqueno;                       
                        (* sign is negative since we reduce the high harmonies*)
                        intensity[j]:=intensity[j]*(1-fractionalchange);                    
                        if(intensity[j]<0)then 
                        begin 
	                        writeln('IllegalIntensity  ',j,' went negative, fractional change = ',fractionalchange);
	                        halt(215);{ signal the pascal arithmetic overflow error }
                        end;
                    end;
                end;
    end;
    

    function computeNetOutput(var C:TechnologyComplex ;var intensity,initial:vector):pvec;
     var outputv :pvec; k,k2,i,j:integer;t:ptechnique;pt:ptvec;it:real;
     begin
        
        outputv :=  computeGrossAvail(  C,  intensity, initial);
        pt:= techniques(C);
        if (verbose) then begin
				writeln('output');
				writeln(outputv^);
		end;
        for j:= 1 to C.techniqueCount do
        begin
            t:= pt^[j];
            it:=intensity[t^.techniqueno];
             
            for k:=1 to t^.consumes^.max do          
                outputv^[t^.consumes^[k].product^.productnumber]:= outputv^[t^.consumes^[k].product^.productnumber]
                       -it*t^.consumes^[k].quantity;                      
             
        end;
        
        computeNetOutput:=outputv;
    end;
    function sigmoid(  d:real):real;
     begin
        if (d>0) then sigmoid:= d/(1+d) else
        if (d=0) then sigmoid:= 0 else begin
        d:= -d;
        sigmoid := -(d/(1+d) );
        end
    end;
    
 begin
 end.
