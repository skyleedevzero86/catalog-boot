import type { CreateConnectionRequest, DatabaseVendor } from '../types/api'

export type VendorDefaults = {
  label: string
  port: number
  databaseName: string
  schemaName: string
  username: string
  password: string
}

export const VENDOR_DEFAULTS: Record<DatabaseVendor, VendorDefaults> = {
  POSTGRESQL: {
    label: 'PostgreSQL',
    port: 5432,
    databaseName: 'cdw',
    schemaName: 'demo',
    username: 'postgres',
    password: 'postgres',
  },
  MYSQL: {
    label: 'MySQL',
    port: 3306,
    databaseName: 'source_db',
    schemaName: '',
    username: 'catalog',
    password: 'catalog',
  },
  MARIADB: {
    label: 'MariaDB',
    port: 3307,
    databaseName: 'source_db',
    schemaName: '',
    username: 'catalog',
    password: 'catalog',
  },
  ORACLE: {
    label: 'Oracle',
    port: 1521,
    databaseName: 'ORCL',
    schemaName: 'HR',
    username: 'hr',
    password: '',
  },
  CLICKHOUSE: {
    label: 'ClickHouse',
    port: 8123,
    databaseName: 'source_db',
    schemaName: '',
    username: 'default',
    password: '',
  },
}

export type LocalDbPreset = {
  id: string
  label: string
  description: string
  request: CreateConnectionRequest
}

export const LOCAL_DB_PRESETS: LocalDbPreset[] = [
  {
    id: 'postgres-demo',
    label: 'PostgreSQL demo',
    description: 'localhost:5432 / cdw / demo (compose)',
    request: {
      name: 'local-postgres-demo',
      vendor: 'POSTGRESQL',
      host: 'localhost',
      port: 5432,
      databaseName: 'cdw',
      schemaName: 'demo',
      description: 'docker-compose postgres sample source (demo.employees)',
      username: 'postgres',
      password: 'postgres',
      enabled: true,
    },
  },
  {
    id: 'mysql',
    label: 'MySQL',
    description: 'localhost:3306 / source_db (compose)',
    request: {
      name: 'local-mysql',
      vendor: 'MYSQL',
      host: 'localhost',
      port: 3306,
      databaseName: 'source_db',
      schemaName: '',
      description: 'docker-compose mysql sample source',
      username: 'catalog',
      password: 'catalog',
      enabled: true,
    },
  },
  {
    id: 'mariadb',
    label: 'MariaDB',
    description: 'localhost:3307 / source_db (compose)',
    request: {
      name: 'local-mariadb',
      vendor: 'MARIADB',
      host: 'localhost',
      port: 3307,
      databaseName: 'source_db',
      schemaName: '',
      description: 'docker-compose mariadb sample source',
      username: 'catalog',
      password: 'catalog',
      enabled: true,
    },
  },
  {
    id: 'clickhouse',
    label: 'ClickHouse',
    description: 'localhost:8123 / source_db (compose)',
    request: {
      name: 'local-clickhouse',
      vendor: 'CLICKHOUSE',
      host: 'localhost',
      port: 8123,
      databaseName: 'source_db',
      schemaName: '',
      description: 'docker-compose clickhouse sample source',
      username: 'default',
      password: '',
      enabled: true,
    },
  },
]

export function formForVendor(vendor: DatabaseVendor): CreateConnectionRequest {
  const defaults = VENDOR_DEFAULTS[vendor]
  return {
    name: '',
    vendor,
    host: 'localhost',
    port: defaults.port,
    databaseName: defaults.databaseName,
    schemaName: defaults.schemaName,
    description: '',
    username: defaults.username,
    password: defaults.password,
    enabled: true,
  }
}
