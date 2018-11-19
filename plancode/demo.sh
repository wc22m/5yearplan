 
  
 set -x
 java planning.nyearplan  flows.csv  cap.csv  dep.csv  labtarg.csv   >test.lp
 
 
wc test.lp  
 
time lp_solve <test.lp |sort >results.txt 

cat results.txt
