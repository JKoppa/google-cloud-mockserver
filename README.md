Quick preliminary readme on how to update mockserver expectations.

1. Fork this repository.

2. Add your new expectations to the `configurations` folder following the practices of other files that are there. You must create a new directory (such as common) and put all your configuration files in it. There is a shell script that will combine all configurations into one and feed it to the mock server during startup

3. Build the docker image locally via 
`docker build -t mockserver .`

4. Run it locally and verify your new expectations work using Postman or cURL or something.
`docker run -p 1080:1080 mockserver`

5. Once functionality has been verified, create a PR to merge your fork into the master branch. 

6. Once merged, Jenkins will automatically deploy the new version of mockserver to Google Cloud and your new expectations should be live.
