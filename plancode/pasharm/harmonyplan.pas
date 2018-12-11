program harmonyplan;
uses  technologies,harmony,csvfilereader;

 type pmat=^matrix;
      channel= record
		p:pcsv;
		r:pheadervec;
		c:pheadervec;
		m:^matrix;
	  end;
var matrices:array[1..4] of pmat;
procedure rf(var ch:channel;param:integer );
(*! Read in one of the file parameters and extract the data from it *)
begin
    with ch do 
    begin
		p:=parsecsvfile(paramstr(param));
		if p=nil then 
		begin
		   writeln('error opening or parsing file ',paramstr(param));
		   halt(2);
		end
		else ;
		r:=getrowheaders(p);
		c:=getcolheaders(p);
		m:=getdatamatrix(p);
		matrices[param]:=m;
	end;
end;

var 
    flows,caps,deps,targs:channel; 
    outputs, compressedDeprates,labour,initialResource,targets,intensities:pvec;
    i,j,k,lr,y,year,maxprod,years,capitals,cn:integer;
    yearXproductIntensityIndex:^matrix;
    relativecapnum:^matrix;
    flow:real;
    t:ptechnique;
    inputs,toutputs:presourcevec;
    capnumtoflownum:pintvec;
  var  C:^TechnologyComplex ; 
    si,sj,sy:string[10];
    start,stop:double;
    function deprate(capitaltype:integer):real;{capitaltype is the compressed capital index}
    begin
        deprate:= compressedDeprates^[capitaltype];
    end;    
    function countyears(heads:pheadervec) :integer;
    var i,j:integer;
    begin
         j:=0;
       for i:= 1 to targs.r^.max do   
       begin
		 
            if(heads^[i]<>nil)then
                if(heads^[i].textual^[1]='y')
                and(heads^[i].textual^[2]='e')
                and(heads^[i].textual^[3]='a')
                and(heads^[i].textual^[4]='r')
                then
                j:=j+1;
        end;
        countyears:=j;
    end;
    function outputrowinheaders:integer;
    var i,j:integer;
    begin
       j:=0;
       for i:= 1 to flows.r^.max do       
            if (flows.r^[i].textual^=('output'))then j:=i;
        if j=0 then 
        begin
			writeln('no output row found in flow matrix');
			halt(301);
		end;
		outputrowinheaders:=j;
    end;
    function labourRow:integer;
    var i,j:integer;
    begin
       j:=0;
       for i:= 1 to flows.r^.max do       
            if (flows.r^[i].textual^=('labour'))then j:=i;
        if j=0 then 
        begin
			writeln('no labour row found in flow matrix');
			halt(301);
		end;
		labourrow:=j;
    end;
    function capnum( prod, year, maxcap:integer) :integer;
    begin
        capnum:= (prod)+(year-1)*(maxcap)+years*(maxprod+1);
    end;
    function countnonzero(var m:matrix):integer;
    var t,i,j:integer;
     begin
        t:=1;
        new(relativecapnum,m.rows,m.cols);
        for i:=1 to m.rows do
	        for j:=1 to m.cols do
                if(m[i][j]>0) then
                begin
                    relativecapnum^[i][j]:=t;
                    t:=t+1;
                end;       
        new(capnumtoflownum,t-1);
        new(compressedDeprates,t-1);
        // pass through again filling in the backwardvector
        //writeln(t);
        t:=1;
        for i:=1 to m.rows do
	        for j:=1 to m.cols do        
                if(m[i][j]>0) then
                begin
                  //  writeln(i,j,t);
                    capnumtoflownum^[t]:=i;
                    compressedDeprates^[t]:=deps.m^[i][j];
                    t:=t+1;
                end;            
        countnonzero:= t-1;
    end;
    function countinputsTo( industry:integer) :integer;
    var total,i:integer;
    begin
        total:=0;
		for i:=1 to maxprod+1 do 
            if(flows.m^[i][industry]>0)then total:=total+1;
        for i:=1 to maxprod do 
            if(caps.m^[i][industry]>0)then total:=total+1;
        countinputsTo:=total;
    end;
    function flownum( prod,  year:integer):integer;
    begin
        flownum:=  (prod)+(year-1)*(maxprod+1);
    end;
    function capname(row,col,year:integer):string;
	begin
			capname:='C['+int2str(row)+']['+int2str(col)+']Y'+int2str(year);
	end;
    function productName(prod,  year,internalcode:integer) :string;
    begin
        productname:=flows.r^[prod].textual^+'Y'+int2str(year)+'{'+int2str(internalcode)+'}';
    end;
    procedure flowsourcedfromTo(year,row,col:integer);
    (*! Generate investment technique starting from the specified year, 
    directed at the specified row and col , with possible joint production *)
    var src,dest:presourcevec;
        outputyears,i:integer;t:ptechnique;
    begin
        
		
		outputyears:= years-year;
		if outputyears >0 then
		begin
		   new (src,1);
		   src^[1].product:= findproduct(C^,productName(row,year,flownum(row,year)));
		   src^[1].quantity:=1;
		   new(dest,outputyears);
		   for i:= 1 to outputyears do
		   with dest^[i] do
		   begin
			 product:=findproduct(C^,capName(row,col,year+i ));
			 quantity:=(1-deps.m^[row][col]) pow (i-1);
		   end;
		   t:= defineTechnique(C^,src^,dest^);
		//   dispose(dest);dispose(src);
		end;
    end;
    procedure flowsOriginatingIn(year:integer);
    (*! This generates all investment flows generated in 'year' .*)
    var r,c:integer;
    begin 
       for r:=1 to maxprod do
         for c:=1 to maxprod do 
           if caps.m^[r][c]>0 then flowsourcedfromTo(year,r,c);
    end;
    procedure printResults(var c:TechnologyComplex; var intensity,  initialResource:vector) ;
    var netoutput,gross,usage ,produced:pvec;toth:real;year:integer;
    procedure writecsvln(var s:headervec) ;
    var i:integer;c:csvcell;
    begin
        for i:=1 to s.max do 
        begin 
         c:= s[i]^;
         with c do
           write(',',textual^);
        end;
        writeln;
    end;
    procedure writecsvvec(var s:vector) ;
    var i:integer; 
    begin
        for i:=1 to s.cols do 
        begin 
           write(',',s[i]);
        end;
        writeln;
    end;
    var row,col,index,i:integer;howmuch,h:real;tv:ptvec;
    begin 
         netoutput:= computeNetOutput(C,intensity,initialResource);
         gross:= computeGrossAvail(  C,   intensity, initialResource);
        writeln('iter,	useweight,	phase2,	temp');
        writeln(' ',iters,',',useweight,',',phase2adjust,',', startingtemp);
        write('year,headings');
        writecsvln(flows.c^); 
        toth:=0;
       for year :=1 to years do
         begin  new(usage,maxprod);new(produced ,maxprod+1);
			
            writeln(year,',flow matrix');
            for row :=1 to outputrowinheaders do
            
            begin 
                write(year);
                write(',',flows.r^[row].textual^);
                for col:=1 to flows.c^.max   do
             
                begin 
                    index := round(yearXproductIntensityIndex^[year][col]);
                    howmuch := intensities^[index]*flows.m^[row][col];
                    write(',',howmuch);
                    if(row<=maxprod)then 
                    begin usage ^[row]:= usage ^[row]+howmuch ;end 
                    else
                    begin produced ^[col]:=howmuch;end;
                end;
                writeln('');
            end;
 
            write(year,',');
            write('productive consumption');
            writecsvvec(usage^);
              write(year,',');
            write('accumulation ');
            for col:=1 to usage^.cols do
             begin 
                write (',',(produced^[col]-netoutput^[flownum(col,year)]-usage^[col]));
            end;
            writeln('');
            write(year,',');
            write('netoutput ');
            for col:=1 to flows.c^.max do
            begin 
                write (',',netoutput^[flownum(col,year)]);
            end;
            writeln('');
            write(year,',');
            write('target  ');
            for col:=1 to flows.c^.max do begin 
                write (',',targs.m^[year][col]);
            end;
            writeln('');
            write(year,',');
            write('netoutput/target  ');
            for col:=1 to flows.c^.max do begin 
                write (',',(netoutput^[flownum(col,year)]/targs.m^[year][col]));
            end;
            writeln('');

            write('',year,',');
            write('harmony  ');
            
            for col:=1 to flows.c^.max do begin 
                h:=Harmony.H(targs.m^[year][col],netoutput^[flownum(col,year)]);
                write (',',h);
                toth:=h+toth;
            end;
            writeln('');
            writeln('',year,',capital use matrix');
            for row := 1 to labourrow-1 do
             begin 
                write('',year);
                write(',',flows.r^[row].textual^);
                for col:=1 to flows.c^.max do  begin 
                    index := round(yearXproductIntensityIndex^[year][col]);
                    howmuch := intensities^[index];
                    write(',',howmuch*caps.m^[row][col]);
                end;
                writeln('');
            end;
            writeln('');
        end;
        tv:=techniques(C);
         for i:=1 to tv^.maxt do
         begin
			 
			writeln(tv^[i]^.techniqueno,'=',tv^[i]^.produces^[1].quantity*intensity[tv^[i]^.techniqueno]);
		end;
        writeln('totalharmony ,',toth);
    end;
    procedure setupintertermporalflow;
    (*! the aim of this procedure is to create techniques which represent
     investment flows, in general these will be joint production techniques.
     We will have one technique for each type of non zero capital good, for each year
      other than the last one. 
    *)
    var y:integer; 
    begin
		for y:=1 to years do flowsOriginatingIn(y);
    end;
    
    var tv:ptvec;
begin
	rf(flows,1);
	rf(caps,2);
	rf(deps,3);
	rf(targs,4);
	   // go through the targets matrix and make sure no targets are actually zero - make them very small positive amounts
	   for i := 1 to targs.m^.rows do
	   for j := 1 to targs.m^.cols do
		  if(targs.m^[i][j]=0) then targs.m^[i][j]:=1 - Harmony.capacitytarget;
		new(outputs,flows.m^.cols);new(labour,flows.m^.cols);
		outputs^ := flows.m^[outputrowinheaders ];
		labour ^:= flows.m^[labourRow];
		years := countyears(targs.r);
		
		maxprod:=flows.m^.cols;
		new(yearXproductIntensityIndex,years,maxprod+2);
		 
		capitals:= countnonzero(caps.m^);
		//writeln('maxprod', maxprod, 'capitals', capitals,' years', years);
		// work out how many products the harmonizer will have to solve for
		// assume that we have N columns in our table and y years then
		// we have Ny year product combinations
		// in addition we have y labour variables
		// and caps.y capital stocks
		// so the total is y(caps+N+1)

	    new(C);	
	    //writeln('call definecomplex(',(maxprod+capitals)*years,')');
	     definecomplex (C^,(maxprod+capitals+1)*years );
		//    writeln("productnum "+C.productCount()+" years "+years);
		// Assign identifiers to the outputs
        for i:=1 to maxprod+1 do
            for year:=1 to years do
				 addproduct( C^,productName(i,year,flownum(i,year)),flownum(i,year) );

		for i := 1 to maxprod do		 
			for j:=1 to maxprod do 			  
			   for year:=1 to years do			  
					if (caps.m^[i][j] >0) then
							 addproduct( C^,capname(i,j,year),capnum(round(relativecapnum^[i][j]),year,capitals) );
							
				 
			 
	 
					 


		for year:= 1 to years do
		begin
			// add a production technology for each definite product
			for i:=1 to maxprod do		 
			begin 
			   // writeln('product ',i,' has ',countinputsTo(i),' inputs to it ');
				new(inputs,countinputsTo(i));
				new(toutputs, 1);			
				j:=1;
			    for k:=1 to maxprod +1 do { include labour}
				begin
					if (flows.m^[k][i]>0) then
					begin
						flow:=flows.m^[k][i];
						inputs^[j].quantity:= flow;
						inputs^[j].product:= findproduct(C^,productName(k,year,flownum(k,year)));
						j:=j+1;
					end;
					if( k<=maxprod)then// no labour row for the capital matrix so we miss last row
						if (caps.m^[k][i]>0)then 
						begin							 
							flow:=caps.m^[k ][i ];
						    inputs^[j].quantity:= flow;							
							inputs^[j].product:=  findproduct(C^,capName(k,i,year ));
							j:= 1+j;
						end;
				end;
				with toutputs^[1] do
				begin
					quantity := outputs^[i];
				{	product:=C^.index^[flownum(i,year)];}
					product := findproduct(C^,productName(i,year,flownum(i,year)));
				end;
				//writeln('year ',year, 'of',years,'first call define techniques');
				t:= defineTechnique(C^, inputs^,toutputs^);	 
				yearXproductIntensityIndex^[year][i]:=C^.techniqueCount;
			end;
		 
		end;
		  
        setupintertermporalflow;
		(*! now set up the initial resource vector *)
		new(initialResource ,C^.productCount);
		(*! put in each years labour *)
		lr := labourRow;
		
		
		
		for y:=1 to years do
		begin
			initialResource ^[flownum(lr,y)]:=targs.m^[y][lr];
			C^.nonproduced^[flownum(lr,y)]:=true;
			C^.nonfinal^[flownum(lr,y)]:=true;
		end;

		(*! put in each years initial capital stock allowing for depreciation *)
		for i:= 1 to caps.m^.rows do
		begin
			for j:=1 to caps.m^.rows do
			begin
				if(caps.m^[i][j]>0)then
					for y :=1 to years do
					begin
						cn:=capnum(round(relativecapnum^[i][j]),y,capitals);
						  if (verbose)then writeln(i,',',j,',',y,',',cn);
						if(y=1)then C^.nonproduced^[cn]:=true;
						C^.nonfinal^[cn]:=true;
						initialResource^ [cn]:=caps.m^[ i][j]* ((1-deps.m^[i][j])pow(y-1));
					end
			end
		end;


		(*!now set up the target vector *)
		new(targets ,C^.productCount);
		// initialise to very small numbers to prevent divide by zero

		targets^:=0.03;
		for y:=1 to years do 
		    for j:= 1 to targs.m^.cols-1 do
			  {do not include the labour col of the targets}
				targets^[flownum(j,y)]:=targs.m^[y][j];
		if( verbose) then
	    begin
         logComplex(C^);
		end;
			 
		start:=secs;
			
		intensities:=balancePlan(  	targets^, 		initialResource   ^,C^);
		stop:=secs;
		printResults(C^, intensities^, initialResource^);
		writeln('took ',((stop-start)*0.01),' sec');


end.
