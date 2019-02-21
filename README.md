Quick preliminary readme on how to update mockserver expectations and push to Google cloud:

1. Add your new expectations to the `configurations` folder following the practices of other files that are there. You must create a new directory (such as common) and put all your configuration files in it. There is a shell script that will combine all configurations into one and feed it to the mock server during startup

2. Build the docker image via 
`docker build -t gcr.io/seventh-chassis-87509/mockserver .`

3. Run it locally and verify your new expectations work using Postman or cURL or something.
`docker run -p 1080:1080 gcr.io/seventh-chassis-87509/mockserver`

4. Push your image to the Google cloud registry (make sure you have installed gcloud sdk and authed the docker registry access)
https://cloud.google.com/container-registry/docs/advanced-authentication
`docker push gcr.io/seventh-chassis-87509/mockserver`

5. Select the test server cluster and project
`gcloud container clusters get-credentials http-bin-cluster-1 --zone us-central1-a --project seventh-chassis-87509`

6. Run the new image (you might have to kill the old one first).
`kubectl run mockserver --image=gcr.io/seventh-chassis-87509/mockserver --port=1080`

The new expectations should now be live.