#/bin/sh

# Create one configuration file which includes the content of every file in the configurations folder
totalConfig="["
firstFile="true"

for f in configurations/**/* ; do
  content=`cat $f`
  if [ "$firstFile" == "true" ] ; 
  then
    totalConfig="$totalConfig$content"
    firstFile="false"
  else
    totalConfig="$totalConfig,$content"
  fi
done;

totalConfig="$totalConfig]"

echo "$totalConfig" > totalConfig.json