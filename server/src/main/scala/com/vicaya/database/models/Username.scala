package com.vicaya.database.models

case class Username(
 userSid: String,
 userName: String,
 accountSid: String,
 timeCreated: Long,
 timeLastLogin: Long,
 isActive: Boolean
)

case class UserPreference (
  userSid: String,
  userPrefSid: String,
  userPrefName: String,
  userPrefLoc: String,
  timeCreated: Long,
  timeUpdated: Long
)

case class Connector (
  accountSid: String,
  connectorSid: String,
  timeCreated: Long,
  timeUpdated: Long,
  timeLastSync: Long
)

case class ConnectorDetails (
  accountSid: String,
  connectorSid: String,
  connectorDetailSid: String,
  connectorType: String,
  connectorCredentials: String,
  timeCreated: Long,
  timeUpdated: Long,
  timeLastSync: Long
)

case class Role(
  roleSid: String,
  roleName: String
)

case class Accounts (
  accountSid: String,
  accountName: String,
  timeCreated: Long,
  timeUpdated: Long,
  isActive: Boolean
)