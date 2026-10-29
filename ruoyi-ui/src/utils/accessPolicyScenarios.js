export const accessPolicyScenarios = [
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
