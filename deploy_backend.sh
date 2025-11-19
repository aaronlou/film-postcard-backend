#!/bin/bash

./mvnw clean package -DskipTests

pm2 stop film-backend  && pm2 delete film-backend

pm2 start "java -jar target/filmpostcard-0.0.1-SNAPSHOT.jar --server.port=8081" --name film-backend

pm2 save

pm2 list
