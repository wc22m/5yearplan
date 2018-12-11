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
     iopair = record
		product:presource;
		quantity:real;
		end;
	 resourcevec(max:integer)=array[1..max] of iopair;
	 presourcevec=^ resourcevec;
	 technique  = record
		produces,consumes:presourcevec;
		
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
     technologycomplex = record
		techniqueslist:ptechniquelist;
		techniquesvec:ptvec;
		index:pproductindex;
		producerIndex , userIndex:pdvec;
		nonfinal:pdvec;
		nonproduced:pbvec;
		 
		techniquecount,productcount:integer;
	 end;
	 tc=technologycomplex;
function techniques(var ct:tc):ptvec;
function buildProducerIndex(var ct:tc):pdvec ;
function buildUserIndex(var ct:tc):pdvec ;
function buildIndex(var ct:tc;produces:boolean):pdvec ;
function defineTechnique(var ct:tc;var inputs,outputs:resourcevec ):ptechnique;
procedure addproduct(var ct:tc;  name:string);
function findproduct(var ct:tc; name:string):presource;
function defineResource(var ct:tc;name:resourceid):presource;
function defineproductlist(var ct:tc;name:string; p:pproductlist):pproductlist;	 
 
function rateOfHarmonyGain(var t:technique;var derivativeOfProductHarmony:vector) :real;
function defineComplex(numberofproducts:integer):technologycomplex;
function  marginalphysicalcoproducts(var t:technique;   input:presource) :pvec;
function getCoproductionCodes(var t :technique):pintvec; 
implementation
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
         if(a[j].product=i)then
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
        
        {  System.out.println(identifier+","+productCode+","+gain+","+cost);}
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

function defineResource(var ct:tc;name:resourceid):presource;
var t:presource;
begin
	new(t);
	with ct do
	with t^ do 
	begin
			id:=name;
			productcount:=productcount+1;
			productNumber:=productcount;
			users:=nil;producers:=nil;
	end;
	defineResource:=t;
end;
function  defineproductlist(var ct:tc;name:string; p:pproductlist):pproductlist;
var pntr:pproductlist;
begin
	new(pntr);
	with ct do begin
	pntr^.next:=p;
	pntr^.product:=defineResource(ct,name);
	defineproductlist:=pntr;
	end;
end;

function  findproduct( var ct:tc; name:string):presource;
var h:integer;p:pproductlist;ok:boolean;
begin
	h:=hash(name) ;
	with ct do 
	begin
	    h := h rem index^.max;
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
		if p=nil then findproduct:=nil
		else findproduct:=p^.product;
	end;
end;
 
procedure  addproduct( var ct:tc; name:string);
var h:integer;p:pproductlist;ok:boolean;
begin
	h:=hash(name) ;
	with ct do
	begin
	    h := h rem index^.max;
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
		if p=nil then index^[h]:= defineproductlist(ct,name,p);
		{ if p<>nil then product already defined }
	end;
end;
		
function  buildIndex(var ct:tc;produces:boolean):pdvec ;
var index:pdvec;I,j,k,l:integer;
    p:ptechniquelist;t:technique;
    producercount:pintvec;
begin
	 begin
        if(ct.producerIndex=nil)then begin
            new( index,ct.productcount );new(producercount, cd.productcount);
            p:=techniqueslist;
            for i := 1 to productcount do producercount^[i]:=0;
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
            for i:=1 to productcount do new(index^[i],producercount^[i]);{ create a vector of producers }
             while p<> nil do
            begin
                  t:=p^.tech^;
                   if produces then l:=t.produces^.max else l:= t.consumes^.max;
                   for i:=1 to l do 
                   begin
                    if produces then j:=t.produces^[i].product^.productnumber
                    else j:=t.consumes^[i].product^.productnumber;
                    k:= producercount^[j];
                    index^[j]^[k]:=p^.tech;
                    producercount^[j]:=k-1;
                   end;
                 p:=p^.next;
            end;
            producerIndex:=index;          
        end;
        buildIndex:= producerIndex;
        end;
end;		 
function  buildProducerIndex(var ct:tc):pdvec ;begin with ct do buildproducerindex:= buildIndex(true); end;
function  buildUserIndex(var ct:tc):pdvec ;	begin with ct do builduserindex:= buildindex(false); end;
function defineComplex(numberofproducts:integer):technologycomplex;
var complex:technologycomplex;
begin
	with complex do
	begin
		new(index,(numberofproducts div 2)+1);
		new (nonproduced, numberofproducts);
		new (nonfinal,numberofproducts);
		 
		techniques:=nil;
		techniquecount:=0;
		productcount:=0;
		index^:=nil
	end;
	defineComplex:=complex;
end;
function  defineTechnique(var ct:tc;var inputs,outputs:resourcevec ):ptechnique;
var t:ptechnique;i:integer; var tl:ptechniquelist;
   procedure adduser(product:presource );
   var l:ptechniquelist;
   begin
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
		new(l);
		with l^ do 
		begin
			tech:=t;
			next:=product^.producers;
		end;
		product^.producers:=l;
	end;  
begin
	new(t);
	with ct do begin
		with t^ do 
		begin
			new(produces,outputs.max);
			new(consumes,inputs.max);
			produces^:=outputs;
			consumes^:=inputs;
			for i:=0 to outputs.max do
				addproducer(outputs[i].product );
		    for i:=0 to inputs.max do
				adduser(inputs[i].product );
		end;
		techniqueCount := techniquecount+1;
		new (tl);
		tl^.tech:=t;
		tl^.next:= techniques;
		techniques:=tl;
		definetechnique:=t;
	end;
end;
begin
  
end.

   
