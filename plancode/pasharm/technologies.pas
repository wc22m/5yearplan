(*! A library to represent a set of production technologies in a more compact
  form than as an input output table or matrix. It can take advantage
  of the sparse character of large io tables.
   
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
    * *)
unit technologies;
interface 

const namelen = 35;
type 
	 pvec=^vector;
     resourceid= string[namelen];
     ptechniquelist = ^ techniquelist;
     resourcerec = record
		id:resourceid;
		productNumber:integer;
		users,producers:ptechniquelist;
	 end;
     presource = ^ resourcerec;
     resourceindex (max:integer)=array[1..max] of presource;
     presourceindex=^resourceindex;
     iopair = record
		product:presource;
		quantity:real;
		end;
	 resourcevec(max:integer)=array[1..max] of iopair;
	 presourcevec=^ resourcevec;
	 technique  = record
		produces,consumes:presourcevec;
		techniqueno:integer;
		end;
	 ptechnique =^technique;
	 techniquelist =record
		tech:ptechnique;
		next:ptechniquelist;
	 end;
	 pproductlist = ^productlist;
	 productlist =record
		product:presource;
		next:pproductlist;
	 end;
	 productindex(max:integer)=array[0..max ] of pproductlist;{ Hash table of products }
	 pproductindex = ^ productindex;
	 intvec(maxi:integer)=array[1..maxi] of integer;
     pintvec=^intvec;
     techvec(maxt:integer)= array [1..maxt]of ptechnique;
     ptvec =^techvec;
     producervec(maxv:integer)= array [1..maxv]of ptvec;
     pdvec=^producervec;
     bvec(max:integer)= array[1..max] of boolean;
     pbvec=^bvec;
     pcomplex=^technologycomplex;
     technologycomplex = record
		techniqueslist:ptechniquelist;
		techniquesvec:ptvec;
		index:pproductindex;
		producerIndex , userIndex:pdvec;
		nonfinal:pbvec;
		nonproduced:pbvec;	 
		techniquecount,productcount:integer;
		 
		allresourceindex:presourceindex;
	 end;
	 tc=technologycomplex;

procedure logComplex(var ct:tc);  
function techniques(var ct:tc):ptvec;
function produces(var ct:tc;t :technique;productNumber:integer):boolean;
function buildProducerIndex(var ct:tc):pdvec ;
function buildUserIndex(var ct:tc):pdvec ;
function buildIndex(var ct:tc;produces:boolean):pdvec ;
function defineTechnique(var ct:tc;var inputs,outputs:resourcevec ):ptechnique;
procedure addproduct(var ct:tc;  name:string;number:integer);
function findproduct(var ct:tc; name:string):presource;
function defineResource(var ct:tc;name:resourceid;number:integer):presource;
function defineproductlist(var ct:tc;name:string; p:pproductlist;number:integer):pproductlist;

procedure defineComplex(var ct:tc;numberofproducts:integer);
function rateOfHarmonyGain(var t:technique;var derivativeOfProductHarmony:vector) :real ;
function  marginalphysicalcoproducts(var t:technique;   input:presource) :pvec;
function getCoproductionCodes(var t :technique):pintvec; 
implementation
 procedure logComplex(var ct:tc);
var f:text;i,j:integer;ui:pdvec
   procedure printtechnique(var t:technique);
   var i:integer;
   begin
    with t do 
    begin
	 writeln(f,'technique,',techniqueno);
	 write(f,'inputs');
	 for i:=1 to consumes^.max do
	 write(f,',',consumes^[i].product^.productnumber);
	 writeln(f);
	 write(f,'outputs');
	 for i:=1 to produces^.max do
	 write(f,',',produces^[i].product^.productnumber);
	 writeln(f);
	end;
   end;
   procedure rect(te:ptechniquelist);
   begin
	 if te=nil then writeln(f)
	 else 
	 begin
		rect(te^.next);
		printtechnique(te^.tech^);
	 end;
	end;
begin
 with ct do begin
	assign(f,'complex.csv');
	rewrite(f);
	writeln(f,'Technology Complex');
	writeln(f,'index.max,nonproduced.max,nonfinal.max,allresourceindex.max,techniquecount, productcount');
	writeln(f,index^.max,',',nonproduced^.max,',',nonfinal^.max,',',allresourceindex^.max,',',techniquecount, ',',productcount);
	write(f,'Resource number');for i:=1 to allresourceindex^.max do write(f,',',i);writeln(f);
	write(f, 'Resource id');
	for i:=1 to allresourceindex^.max do
	  if allresourceindex^[i]=nil then write (f,', ') else write(f,',',allresourceindex^[i]^.id);
	writeln(f);
	{ now list all techniques }
	rect(techniqueslist);
	{ now the user index }
	writeln(f,'User index');
	ui:=buildUserIndex(ct); 
	for i:=1 to ui^.maxv do 
	begin
	  write(f,'Product,',i,',is used by technique');
	  for j:=1 to ui^[i]^.maxt do
	    write(f,',',ui^[i]^[j]^.techniqueno);
	  writeln(f);
	end;
	writeln(f,'Producer index');
	ui:=buildproducerIndex(ct);
	for i:=1 to ui^.maxv do 
	begin
	  write(f,'Product,',i);
      if nonproduced^[i] then write(f,', is an initial input and produced by') else write(f,',is produced by technique');
	  for j:=1 to ui^[i]^.maxt do
	    write(f,',',ui^[i]^[j]^.techniqueno);
	  writeln(f);
	end; 
	close(f);
  end;
end;

function  techniques(var ct:tc):ptvec;
var i:integer; list:ptechniquelist;
begin
  with ct do begin
	if techniquesvec=nil then 
	begin
		new(techniquesvec,techniquecount);
		list:=techniqueslist;
		for i:=1 to techniquecount do
		begin
			techniquesvec^[i]:=list^.tech; 
			list:=list^.next;
		end;
	end;
	techniques:=techniquesvec;
	end;
end;
function  produces(var ct:tc;t :technique;productNumber:integer):boolean;
var i:integer;ok:boolean;
begin
 with t do with ct do
 begin
   ok:=false;
   for i:=1 to produces^.max do
     if produces^[i].product^.productnumber =productNumber then ok:=true;
 end;
 produces:=ok;
end;
function getCoproductionCodes(var t :technique):pintvec; 
var p:pintvec;
begin
 with t do
 begin
	new(p,produces^.max);
	p^:= produces^[iota[0]].product^.productNumber;
	getCoproductionCodes:=p;
 end;
end;
function findiIna(  i:presource;var a: resourcevec) :integer;
label 99;
var j:integer;
begin
        for j:=1 to a.max do
         if(a[j].product^.id=i^.id)then
          begin 
          findiIna := j ;
          goto 99 ; 
          end;
        findiIna:=-1;
        99:
end;
function  marginalphysicalcoproducts(var t:technique;    input:presource) :pvec;
var mpp:pvec;pos,i :integer;
begin
        new(mpp, t.produces^.max);
         
        pos :=findiIna(input,  t.consumes^);
        if pos<1 then 
        begin
            writeln('findiIna returns ',pos);
            if input = nil then write('input was nil') else writeln('input non nil');
            writeln(' in  technique ',t.techniqueno);
			writeln('could not find ',input^.productnumber, input^.id);
			writeln('the technique actually consumes the following');
			for i:=1 to t.consumes^.max do
			write(t.consumes^[i].product^.id,', ');
			halt(405);
        end;
        with t do
        for i:=1 to mpp^.cols do       
            mpp^[i]:=produces^[i].quantity/consumes^[pos].quantity;
        
        marginalphysicalcoproducts:=mpp;
end;
function  rateOfHarmonyGain(var t:technique;var derivativeOfProductHarmony:vector) :real;
var gain,cost:real; j:integer;
begin    with t do begin
         gain:=0; 
         for j:=1 to produces^.max do 
           gain := gain + derivativeOfProductHarmony[produces^[j].product^. productNumber]* produces^[j].quantity;
         cost:=0;
         for j:=1 to consumes^.max do 
           cost:=cost+derivativeOfProductHarmony[ consumes^[j].product^. productNumber]*  consumes^[j].quantity;
        
         {  writeln(techniqueno,',',produces^[1].product^. productNumber,', ',gain,',',cost);{}
        rateofharmonygain:= (gain-cost)/cost;
        end;
end;
function hash(s:string):integer;
var i,j:integer;
begin
	j:=1;
	for i:= 1 to length(s) do
	  j:= (j*11 +ord(s[i]) ) and maxint;
	hash:=j;
end;

function  defineResource(var ct:tc;name:resourceid;number:integer):presource;
var t:presource;  
begin
	new(t);
	with t^ do with ct do
	begin
			id:=name;
			productcount:=number;
			productNumber:=productcount;
			users:=nil;producers:=nil;
		 
	        allresourceindex^[productnumber]:=t;
			 
	end;
	
	defineResource:=t;
end;
function  defineproductlist(var ct:tc;name:string; p:pproductlist;number:integer):pproductlist;
var pntr:pproductlist;
begin
  {  writeln('defineproductlist ',name,number);{}
	new(pntr);
	pntr^.next:=p;
	pntr^.product:=defineResource(ct,name,number);
	defineproductlist:=pntr;
	{writeln('defined');{}
end;

function  findproduct(var ct:tc;  name:string):presource;
var h,hm:integer;p:pproductlist;ok:boolean;
begin
   
	h:=hash(name) ;
	with ct do
	begin
	    hm:= index^.max;
	    h := h rem hm;
	         
		p:=index^[h];
		ok:= p<>nil;
		while ok do 
	    begin
	        
			ok := not (p^.product^.id = name);
			if ok then 
			begin
				p:= p^.next;
				ok:= p<> nil;
			end;
		end;
		if p=nil then 
		begin
	{	writeln('product ',name,' not found');}
		exit(401);
		end;
		  
		if p=nil then findproduct:=nil
		else findproduct:=p^.product;
	end;
end;
 
procedure  addproduct( var ct:tc; name:string;number:integer);
var h,hm:integer;p:pproductlist;ok:boolean;
begin
  {  writeln(' add product ', name,number);}
	h:=hash(name) ;
	 
	with ct do
	begin
	   
	   hm:= index^.max;
       
	    h := h rem hm;
	  
		p:=index^[h];
		ok:= p<>nil;
		while ok do 
	    begin
			ok := p^.product^.id <> name;
			if ok then 
			begin
				p:= p^.next;
				ok:= p<> nil;
			end;
		end;
		 
		if p=nil then index^[h]:= defineproductlist(ct,name,index^[h],number);
		{ if p<>nil then product already defined }
	end;
	{writeln('added');}
end;
		
function  buildIndex(var ct:tc;produces:boolean):pdvec ;
var locindex:pdvec;I,j,k,l:integer;p:ptechniquelist;t:technique;
    producercount:pintvec;
begin
  with ct do
  begin
         
        if((producerIndex=nil)and produces)or ((userIndex=nil)and not produces)then 
        begin
        
            new( locindex,productcount );new(producercount, productcount);
            p:=techniqueslist;
            producercount^ :=0;
            while p<> nil do
            begin
                  t:=p^.tech^;
                  if produces then l:=t.produces^.max else l:= t.consumes^.max;
                 
                  for i:=1 to l do 
                  begin  
                    if produces then k:=t.produces^[i].product^.productnumber
                    else k:=t.consumes^[i].product^.productnumber;                   
                    producercount^[k]:=  producercount^[k]+1;
                  end;
                  p:=p^.next;
            end;
  
            p:=techniqueslist;
            for i:=1 to productcount do new(locindex^[i],producercount^[i]);{ create a vector of producers }
            while p<> nil do
            begin
                  t:=p^.tech^;
                   if produces then l:=t.produces^.max else l:= t.consumes^.max;
                   for i:=1 to l do 
                   begin
                    if produces then j:=t.produces^[i].product^.productnumber
                    else j:=t.consumes^[i].product^.productnumber;
                    k:= producercount^[j];
                  
                    locindex^[j]^[k]:=p^.tech;
                    producercount^[j]:=k-1;
                   end;
                 p:=p^.next;
            end;
            if produces then
            producerIndex:=locindex
            else userindex:= locindex        
        end;
        if produces then
        buildIndex:= producerIndex
        else buildIndex:= userindex;
	end;
end;		 
function  buildProducerIndex(var ct:tc):pdvec ;begin buildproducerindex:= buildIndex(ct,true); end;
function  buildUserIndex(var ct:tc):pdvec ;	begin builduserindex:= buildindex(ct,false); end;
procedure  defineComplex(var ct:tc;numberofproducts:integer);
var complex:technologycomplex;
begin
  { writeln('definecomplex ',numberofproducts);}
    with ct do
	begin
		new(index,(numberofproducts div 2)+1);
	{	writeln(index^.max);}
		new (nonproduced, numberofproducts);
		nonproduced^:=false;
		new (nonfinal,numberofproducts);
		nonfinal^:=false;
		 techniquesvec:=nil;producerIndex :=nil; userIndex:=nil;
		techniqueslist:=nil;
		techniquecount:=0;
		productcount:=0;
	    
		new(allresourceindex,numberofproducts);
		allresourceindex^:=nil;
		index^:=nil
	end;
	 
end;

function  defineTechnique(var ct:tc;var inputs,outputs:resourcevec ):ptechnique;
var t:ptechnique;i:integer; var tl:ptechniquelist;
   procedure adduser(product:presource );
   var l:ptechniquelist;
   begin
    // writeln('adduser ',product^.productnumber, product^.id);
		new(l);
		with l^ do 
		begin
			tech:=t;
			next:=product^.users;
		end;
		product^.users:=l;
	end;
   procedure addproducer(product:presource );
   var l:ptechniquelist;
   begin
      //  writeln('addproducer ',product^.productnumber, product^.id);
		new(l);
		with l^ do 
		begin
			tech:=t;
			next:=product^.producers;
		end;
		product^.producers:=l;
	end;  
begin
 with ct do
 begin
	new(t);
	techniqueCount := techniqueCount+1;
	//writeln('def tech ',techniquecount, ' with ', inputs.max,' inputs and ',outputs.max,' outputs');
	
	with t^ do
	begin
	    techniqueno:=techniquecount;
		new(produces,outputs.max);
		
		new(consumes,inputs.max);
		produces^:=outputs;
		consumes^:=inputs;
		for i:=1 to outputs.max do
		    if outputs[i].product = nil then 
		    begin writeln('null product in outputs',i) ;halt(300); end 
		    else
			addproducer(outputs[i].product );
	    for i:=1 to inputs.max do
	     if inputs[i].product = nil then 
	     begin writeln('null product in inputs',i) ; halt(300); end else
			adduser(inputs[i].product );
	end;
	
	new (tl);
	tl^.tech:=t;
	tl^.next:= techniqueslist;
	techniqueslist:=tl;
	definetechnique:=t;
 end;
end;
begin
  
end.

   
