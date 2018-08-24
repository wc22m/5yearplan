 
# java planning.testeconomy $1 $2
 echo harmony alg
time java planning.nyearHarmony testflow.csv testcap.csv testdep.csv testtarg.csv    >harmony.txt
 echo prepass to prepare lp
time  java planning.nyearplan testflow.csv testcap.csv testdep.csv testtarg.csv   >test.lp
 
 
wc test.lp  
 echo lp time
time lp_solve <test.lp|sort >test.txt 

 

