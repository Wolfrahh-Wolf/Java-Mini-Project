#!/bin/bash
sqlplus -s garage_user/GaragePass#2025@//localhost:1521/FREEPDB1 @/opt/oracle/scripts/schema.sql