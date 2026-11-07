export const accessPolicyScenarios = [
  { code: 'ACCESS_OFFICE_ALLOW', name: '低风险办公网允许规则', effect: '0', priority: 5, types: ['IP', 'RISK'], networks: '192.168.10.0/24', maxRisk: 'LOW', levels: ['SECRET'], description: '基础密级和知悉组满足后，按优先级允许低风险办公网访问' },
  { code: 'ACCESS_OFFICE_NETWORK', name: '办公网来源限制', types: ['IP'], networks: '192.168.10.0/24', levels: [], description: '仅允许登记的办公网来源访问，需按实际网络调整网段' },
  { code: 'ACCESS_SECRET_NETWORK', name: '秘密资料指定网络访问', types: ['IP'], networks: '10.20.0.0/16', levels: ['SECRET'], description: '秘密资料仅在指定网络内查阅' },
  { code: 'ACCESS_LOW_RISK_ONLY', name: '低风险人员查阅', types: ['RISK'], maxRisk: 'LOW', levels: [], description: '拒绝中、高风险人员访问所选资料' },
  { code: 'ACCESS_CONFIDENTIAL_RISK', name: '机密资料风险约束', types: ['RISK'], maxRisk: 'LOW', levels: ['CONFIDENTIAL'], description: '机密资料附加低风险要求，密级和知悉组仍须满足' },
  { code: 'ACCESS_OFFICE_COMBINED', name: '办公时段与来源联合限制', types: ['TIME', 'IP'], start: '08:30', end: '18:00', networks: '192.168.10.0/24', levels: [], description: '访问必须同时满足工作时段与办公网来源' },
  { code: 'ACCESS_SECRET_COMBINED', name: '秘密资料联合限制', types: ['TIME', 'IP', 'RISK'], start: '09:00', end: '18:00', networks: '10.20.0.0/16', maxRisk: 'LOW', levels: ['SECRET'], description: '同时满足指定时段、指定网段和低风险要求' },
  {
    'code': 'ACCESS_OFFICE_DAY',
    'name': '日常办公访问',
    'start': '08:30',
    'end': '18:00',
    'levels': [],
    'description': '适用于日间办公人员'
  },
  {
    'code': 'ACCESS_RESEARCH_SHIFT',
    'name': '研发延时访问',
    'start': '08:00',
    'end': '20:00',
    'levels': [],
    'description': '适用于研发长班人员'
  },
  {
    'code': 'ACCESS_OPERATIONS_NIGHT',
    'name': '运维夜班访问',
    'start': '20:00',
    'end': '08:00',
    'levels': [],
    'description': '适用于跨日值班人员'
  },
  {
    'code': 'ACCESS_CONTRACTOR_DAY',
    'name': '外协日间访问',
    'start': '09:00',
    'end': '17:00',
    'levels': [],
    'description': '适用于受托人员的日间工作'
  },
  {
    'code': 'ACCESS_INTERNAL_FINANCE',
    'name': '财务人员内部资料访问',
    'start': '08:30',
    'end': '18:30',
    'levels': [
      'INTERNAL'
    ],
    'description': '面向财务人员的内部级文档访问时段'
  },
  {
    'code': 'ACCESS_INTERNAL_PERSONNEL',
    'name': '人事人员内部资料访问',
    'start': '09:00',
    'end': '18:00',
    'levels': [
      'INTERNAL'
    ],
    'description': '面向人事人员的内部级文档访问时段'
  },
  {
    'code': 'ACCESS_SECRET_PROJECT',
    'name': '项目成员秘密资料访问',
    'start': '09:00',
    'end': '18:00',
    'levels': [
      'SECRET'
    ],
    'description': '面向项目成员的秘密级文档访问时段'
  },
  {
    'code': 'ACCESS_CONFIDENTIAL_REVIEW',
    'name': '机密资料查阅',
    'start': '09:00',
    'end': '17:00',
    'levels': [
      'CONFIDENTIAL'
    ],
    'description': '限定机密级资料查阅时段'
  },
  {
    'code': 'ACCESS_PUBLIC_SERVICE',
    'name': '公开知识服务时段',
    'start': '06:00',
    'end': '23:00',
    'levels': [
      'PUBLIC'
    ],
    'description': '限定公开知识服务开放时段'
  },
  {
    'code': 'ACCESS_INTERNAL_EARLY',
    'name': '内部资料早班访问',
    'start': '06:00',
    'end': '14:00',
    'levels': [
      'INTERNAL'
    ],
    'description': '适用于早班人员的内部资料查阅'
  },
  {
    'code': 'ACCESS_SECRET_NIGHT',
    'name': '秘密资料夜班访问',
    'start': '20:00',
    'end': '08:00',
    'levels': [
      'SECRET'
    ],
    'description': '适用于秘密级资料的夜班处理'
  },
  {
    'code': 'ACCESS_CONFIDENTIAL_NIGHT',
    'name': '机密资料值守访问',
    'start': '22:00',
    'end': '06:00',
    'levels': [
      'CONFIDENTIAL'
    ],
    'description': '适用于机密级资料的夜间值守'
  }
]
