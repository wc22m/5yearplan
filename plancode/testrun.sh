 
 java planning.testeconomy $1 $2
 
 java planning.nyearplan testflow.csv testcap.csv testdep.csv testtarg.csv   >test.lp
 
echo sectors $1 years $2
wc test.lp  
 
time lp_solve <test.lp >test.txt 

