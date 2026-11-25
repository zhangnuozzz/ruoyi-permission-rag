export const accessPolicyScenarios = [
  { code: 'ACCESS_OFFICE_ALLOW', name: '低风险办公网允许规则', effect: '0', priority: 5, types: ['IP', 'RISK'], networks: '192.168.10.0/24', maxRisk: 'LOW', levels: ['SECRET'], description: '基础密级和知悉组满足后，按优先级允许低风险办公网访问' },
  { code: 'ACCESS_OFFICE_NETWORK', name: '办公网来源限制', types: ['IP'], networks: '192.168.10.0/24', levels: [], description: '仅允许登记的办公网来源访问，需按实际网络调整网段' },
  { code: 'ACCESS_SECRET_NETWORK', name: '秘密资料指定网络访问', types: ['IP'], networks: '10.20.0.0/16', levels: ['SECRET'], description: '秘密资料仅在指定网络内查阅' },
  { code: 'ACCESS_LOW_RISK_ONLY', name: '低风险人员查阅', types: ['RISK'], maxRisk: 'LOW', levels: [], description: '拒绝中、高风险人员访问所选资料' },
  { code: 'ACCESS_CONFIDENTIAL_RISK', name: '机密资料风险约束', types: ['RISK'], maxRisk: 'LOW', levels: ['CONFIDENTIAL'], description: '机密资料附加低风险要求，密级和知悉组仍须满足' },
  { code: 'ACCESS_OFFICE_COMBINED', name: '办公时段与来源联合限制', types: ['TIME', 'IP'], start: '08:30', end: '18:00', networks: '192.168.10.0/24', levels: [], description: '访问必须同时满足工作时段与办公网来源' },
  { code: 'ACCESS_SECRET_COMBINED', name: '秘密资料联合限制', types: ['TIME', 'IP', 'RISK'], start: '09:00', end: '18:00', networks: '10.20.0.0/16', maxRisk: 'LOW', levels: ['SECRET'], description: '同时满足指定时段、指定网段和低风险要求' },
  {
    code: 'ACCESS_OFFICE_DAY',
    name: '日间办公网访问',
    types: ['TIME', 'IP'],
    start: '08:30',
    end: '18:00',
    networks: '192.0.2.0/24',
    levels: [],
    description: '办公时段和办公网来源均须满足，绑定前替换示例网段。'
  },
  {
    code: 'ACCESS_RESEARCH_SHIFT',
    name: '研发资料来源网段限制',
    types: ['IP'],
    networks: '198.51.100.0/24',
    levels: [],
    description: '仅允许来自研发网的访问，绑定前替换示例网段。'
  },
  {
    code: 'ACCESS_OPERATIONS_NIGHT',
    name: '运维夜班访问',
    types: ['TIME'],
    start: '20:00',
    end: '08:00',
    levels: [],
    description: '适用于跨日值班人员'
  },
  {
    code: 'ACCESS_CONTRACTOR_DAY',
    name: '外协访问来源与风险限制',
    types: ['IP', 'RISK'],
    networks: '203.0.113.0/27',
    maxRisk: 'LOW',
    levels: [],
    description: '外协访问限约定网段及低风险人员，绑定前替换示例网段。'
  },
  {
    code: 'ACCESS_INTERNAL_FINANCE',
    name: '财务内部资料来源与风险限制',
    types: ['IP', 'RISK'],
    networks: '192.0.2.32/27',
    maxRisk: 'LOW',
    levels: ['INTERNAL'],
    description: '内部财务资料限定专用网段及低风险人员，绑定前替换示例网段。'
  },
  {
    code: 'ACCESS_INTERNAL_PERSONNEL',
    name: '人事内部资料风险限制',
    types: ['RISK'],
    maxRisk: 'LOW',
    levels: ['INTERNAL'],
    description: '中、高风险人员不得访问内部级资料。'
  },
  {
    code: 'ACCESS_SECRET_PROJECT',
    name: '项目秘密资料来源与风险限制',
    types: ['IP', 'RISK'],
    networks: '198.51.100.64/26',
    maxRisk: 'MEDIUM',
    levels: ['SECRET'],
    description: '秘密资料限项目网段及中风险以下人员，绑定前替换示例网段。'
  },
  {
    code: 'ACCESS_CONFIDENTIAL_REVIEW',
    name: '机密查阅风险限制',
    types: ['RISK'],
    maxRisk: 'LOW',
    levels: ['CONFIDENTIAL'],
    description: '机密查阅要求人员为低风险，基础密级与知悉组仍须满足。'
  },
  {
    code: 'ACCESS_PUBLIC_SERVICE',
    name: '公开知识服务时段',
    types: ['TIME'],
    start: '06:00',
    end: '23:00',
    levels: ['PUBLIC'],
    description: '限定公开知识服务开放时段'
  },
  {
    code: 'ACCESS_INTERNAL_EARLY',
    name: '内部资料早班网段限制',
    types: ['TIME', 'IP'],
    start: '06:00',
    end: '14:00',
    networks: '192.0.2.64/26',
    levels: ['INTERNAL'],
    description: '早班人员须在规定时段和网段内访问内部资料，绑定前替换示例网段。'
  },
  {
    code: 'ACCESS_SECRET_NIGHT',
    name: '秘密资料夜班风险限制',
    types: ['TIME', 'RISK'],
    start: '20:00',
    end: '08:00',
    maxRisk: 'LOW',
    levels: ['SECRET'],
    description: '秘密资料夜间访问要求值守时段及低风险。'
  },
  {
    code: 'ACCESS_CONFIDENTIAL_NIGHT',
    name: '机密值守三条件限制',
    types: ['TIME', 'IP', 'RISK'],
    start: '22:00',
    end: '06:00',
    networks: '203.0.113.64/26',
    maxRisk: 'LOW',
    levels: ['CONFIDENTIAL'],
    description: '机密值守须同时满足时段、专用网段及低风险，绑定前替换示例网段。'
  }
]
