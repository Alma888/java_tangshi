// var 是 variable
// document 是 js 中的 html 文档对象
// document.getElementById('main')) 找到 html 文档中的 id 为 main 的元素
// echarts.init 初始化画布元素
var myChart = echarts.init(document.getElementById('main'));

// 指定图表的配置项和数据
var option = {
    // 图标的标题
    title: {
        text: 'ECharts 入门示例'
    },
    tooltip: {},
    legend: {
        data:['数量']
    },
    // 横坐标
    xAxis: {
        data: ["李白","杜甫","王维","李商隐","白居易","王昌龄"]
    },
    yAxis: {},
    series: [
        {
            name: '数量',
            type: 'bar',    // bar 代表柱状图
            itemStyle: {
                color: new echarts.graphic.LinearGradient(
                    0, 0, 0, 1,
                    [
                        {offset: 0, color: '#83bff6'},
                        {offset: 0.5, color: '#188df0'},
                        {offset: 1, color: '#188df0'}
                    ]
                )
            },
            emphasis: {
                itemStyle: {
                    color: new echarts.graphic.LinearGradient(
                        0, 0, 0, 1,
                        [
                            {offset: 0, color: '#2378f7'},
                            {offset: 0.7, color: '#2378f7'},
                            {offset: 1, color: '#83bff6'}
                        ]
                    )
                }
            },
            data: [5, 20, 36, 10, 10, 20] // 对应的每一个的数据
        }
    ]
};

myChart.setOption(option);

// 当前的数据全部是在前端写死的，我需要通过一个 http 请求，从后端获取所有数据
// jquery
