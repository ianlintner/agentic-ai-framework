/**
 * Agentic AI Framework - Web Dashboard
 * Main JavaScript file for dashboard functionality with enhanced debugging visualizations
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize all dashboard components
    initializeNavigation();
    initializeMemoryVisualization();
    initializeTaskForm();
    initializeWebSocket();
    initializeMemoryInspector();
    initializeDocumentationNavigation();
    
    // Enhanced debugging visualizations
    createMemoryChart();
    createMemoryGraph();
    createAgentDecisionTree();
    createPerformanceMetrics();
    createAgentStateTimeline();
});

/**
 * Navigation between dashboard views
 */
function initializeNavigation() {
    const navLinks = document.querySelectorAll('nav a');
    const views = document.querySelectorAll('.view');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all links and views
            navLinks.forEach(link => link.classList.remove('active'));
            views.forEach(view => view.classList.remove('active'));
            
            // Add active class to clicked link
            this.classList.add('active');
            
            // Show the corresponding view
            const viewId = this.getAttribute('data-view') + '-view';
            document.getElementById(viewId).classList.add('active');
        });
    });
}

/**
 * Memory chart visualization - Enhanced for better debugging
 */
function createMemoryChart() {
    // Sample data for memory usage
    const data = [
        { date: new Date('2025-04-01T08:00:00'), total: 90, active: 60, inactive: 30, compressed: 15 },
        { date: new Date('2025-04-01T10:00:00'), total: 100, active: 65, inactive: 35, compressed: 18 },
        { date: new Date('2025-04-01T12:00:00'), total: 120, active: 80, inactive: 40, compressed: 22 },
        { date: new Date('2025-04-01T14:00:00'), total: 124, active: 87, inactive: 37, compressed: 25 },
    ];
    
    const chartContainer = document.getElementById('memory-chart');
    if (!chartContainer) return;
    
    const width = chartContainer.clientWidth;
    const height = chartContainer.clientHeight;
    const margin = { top: 20, right: 60, bottom: 30, left: 40 };
    const innerWidth = width - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;
    
    // Create SVG element
    const svg = d3.select('#memory-chart')
        .append('svg')
        .attr('width', width)
        .attr('height', height);
    
    // Create group inside SVG
    const g = svg.append('g')
        .attr('transform', `translate(${margin.left},${margin.top})`);
    
    // Create scales
    const xScale = d3.scaleTime()
        .domain(d3.extent(data, d => d.date))
        .range([0, innerWidth]);
    
    const yScale = d3.scaleLinear()
        .domain([0, d3.max(data, d => d.total) * 1.1])
        .range([innerHeight, 0]);
    
    // Create line generators
    const lineTotal = d3.line()
        .x(d => xScale(d.date))
        .y(d => yScale(d.total))
        .curve(d3.curveMonotoneX);
    
    const lineActive = d3.line()
        .x(d => xScale(d.date))
        .y(d => yScale(d.active))
        .curve(d3.curveMonotoneX);
        
    const lineInactive = d3.line()
        .x(d => xScale(d.date))
        .y(d => yScale(d.inactive))
        .curve(d3.curveMonotoneX);
    
    const lineCompressed = d3.line()
        .x(d => xScale(d.date))
        .y(d => yScale(d.compressed))
        .curve(d3.curveMonotoneX);
    
    // Add X-axis
    g.append('g')
        .attr('transform', `translate(0, ${innerHeight})`)
        .call(d3.axisBottom(xScale).ticks(4).tickFormat(d3.timeFormat('%H:%M')));
    
    // Add Y-axis
    g.append('g')
        .call(d3.axisLeft(yScale));
    
    // Add Y-axis label
    g.append('text')
        .attr('transform', 'rotate(-90)')
        .attr('y', -margin.left + 10)
        .attr('x', -innerHeight / 2)
        .attr('text-anchor', 'middle')
        .text('Memory Cells Count');
    
    // Add total memory line
    g.append('path')
        .datum(data)
        .attr('fill', 'none')
        .attr('stroke', '#6c7293')
        .attr('stroke-width', 2)
        .attr('d', lineTotal);
    
    // Add active memory line
    g.append('path')
        .datum(data)
        .attr('fill', 'none')
        .attr('stroke', '#4a6cf7')
        .attr('stroke-width', 2)
        .attr('d', lineActive);
    
    // Add inactive memory line
    g.append('path')
        .datum(data)
        .attr('fill', 'none')
        .attr('stroke', '#ffbc00')
        .attr('stroke-width', 2)
        .attr('d', lineInactive);
    
    // Add compressed memory line
    g.append('path')
        .datum(data)
        .attr('fill', 'none')
        .attr('stroke', '#0acf97')
        .attr('stroke-width', 2)
        .attr('d', lineCompressed);
    
    // Add circles for data points
    const circleGroups = ['total', 'active', 'inactive', 'compressed'];
    const colors = ['#6c7293', '#4a6cf7', '#ffbc00', '#0acf97'];
    
    circleGroups.forEach((group, i) => {
        g.selectAll(`.dot-${group}`)
            .data(data)
            .enter().append('circle')
            .attr('class', `dot-${group}`)
            .attr('cx', d => xScale(d.date))
            .attr('cy', d => yScale(d[group]))
            .attr('r', 4)
            .attr('fill', colors[i])
            .append('title')
            .text(d => `${group}: ${d[group]} cells at ${d.date.toLocaleTimeString()}`);
    });
    
    // Add legend
    const legend = svg.append('g')
        .attr('transform', `translate(${width - 120}, 10)`);
    
    const legendData = [
        { label: 'Total Cells', color: '#6c7293' },
        { label: 'Active Cells', color: '#4a6cf7' },
        { label: 'Inactive Cells', color: '#ffbc00' },
        { label: 'Compressed', color: '#0acf97' }
    ];
    
    legendData.forEach((item, i) => {
        const legendRow = legend.append('g')
            .attr('transform', `translate(0, ${i * 20})`);
        
        legendRow.append('rect')
            .attr('width', 10)
            .attr('height', 10)
            .attr('fill', item.color);
        
        legendRow.append('text')
            .attr('x', 15)
            .attr('y', 10)
            .text(item.label)
            .style('font-size', '12px');
    });
}

/**
 * Memory visualization graph with enhanced debugging information
 */
function createMemoryGraph() {
    // Enhanced sample data for memory cells
    const nodes = [
        { id: "cell1", label: "Task Request", group: "request", size: 20, type: "TaskRequest", priority: "High", tags: ["research", "quantum"] },
        { id: "cell2", label: "Task Response", group: "response", size: 18, type: "TaskResponse", status: "Completed", subtaskCount: 3 },
        { id: "cell3", label: "Subtask 1", group: "subtask", size: 15, type: "Subtask", status: "Completed", dependencies: 1 },
        { id: "cell4", label: "Subtask 2", group: "subtask", size: 15, type: "Subtask", status: "Completed", dependencies: 1 },
        { id: "cell5", label: "Subtask 3", group: "subtask", size: 15, type: "Subtask", status: "Completed", dependencies: 1 },
        { id: "cell6", label: "Result 1", group: "result", size: 12, type: "SubtaskResult", status: "Completed", processingTime: "0.5s" },
        { id: "cell7", label: "Result 2", group: "result", size: 12, type: "SubtaskResult", status: "Completed", processingTime: "0.7s" },
        { id: "cell8", label: "Result 3", group: "result", size: 12, type: "SubtaskResult", status: "Completed", processingTime: "0.3s" },
        { id: "cell9", label: "Context", group: "context", size: 25, type: "Context", scope: "Global", usage: "High" },
        { id: "cell10", label: "System Config", group: "system", size: 18, type: "Config", scope: "Global", modified: false },
        { id: "cell11", label: "Agent State", group: "agent", size: 22, type: "AgentState", status: "Active", lastTransition: "Processing" }
    ];
    
    const links = [
        { source: "cell1", target: "cell3", value: 1, label: "parent" },
        { source: "cell1", target: "cell4", value: 1, label: "parent" },
        { source: "cell1", target: "cell5", value: 1, label: "parent" },
        { source: "cell3", target: "cell6", value: 1, label: "produces" },
        { source: "cell4", target: "cell7", value: 1, label: "produces" },
        { source: "cell5", target: "cell8", value: 1, label: "produces" },
        { source: "cell6", target: "cell2", value: 1, label: "contributes" },
        { source: "cell7", target: "cell2", value: 1, label: "contributes" },
        { source: "cell8", target: "cell2", value: 1, label: "contributes" },
        { source: "cell9", target: "cell1", value: 1, label: "informs" },
        { source: "cell9", target: "cell3", value: 0.5, label: "informs" },
        { source: "cell9", target: "cell4", value: 0.5, label: "informs" },
        { source: "cell9", target: "cell5", value: 0.5, label: "informs" },
        { source: "cell10", target: "cell11", value: 0.8, label: "configures" },
        { source: "cell11", target: "cell1", value: 1, label: "processes" },
        { source: "cell11", target: "cell2", value: 0.5, label: "produces" }
    ];
    
    const graphContainer = document.getElementById('memory-graph');
    if (!graphContainer) return;
    
    const width = graphContainer.clientWidth;
    const height = graphContainer.clientHeight;
    
    // Colors for different node groups
    const colors = {
        "request": "#4a6cf7",
        "response": "#0acf97",
        "subtask": "#ffbc00",
        "result": "#39afd1",
        "context": "#fa5c7c",
        "system": "#6c7293",
        "agent": "#2b908f"
    };
    
    // Create SVG element
    const svg = d3.select('#memory-graph')
        .append('svg')
        .attr('width', width)
        .attr('height', height);
    
    // Add zoom functionality
    const zoom = d3.zoom()
        .scaleExtent([0.5, 3])
        .on('zoom', (event) => {
            g.attr('transform', event.transform);
        });
    
    svg.call(zoom);
    
    const g = svg.append('g');
    
    // Create force simulation
    const simulation = d3.forceSimulation(nodes)
        .force('link', d3.forceLink(links).id(d => d.id).distance(100))
        .force('charge', d3.forceManyBody().strength(-200))
        .force('center', d3.forceCenter(width / 2, height / 2))
        .force('collision', d3.forceCollide().radius(d => d.size + 10));
    
    // Create link arrows
    svg.append('defs').selectAll('marker')
        .data(['end'])
        .enter().append('marker')
        .attr('id', 'arrow')
        .attr('viewBox', '0 -5 10 10')
        .attr('refX', 25)
        .attr('refY', 0)
        .attr('markerWidth', 6)
        .attr('markerHeight', 6)
        .attr('orient', 'auto')
        .append('path')
        .attr('d', 'M0,-5L10,0L0,5')
        .attr('fill', '#999');
    
    // Create links with labels
    const link = g.append('g')
        .attr('class', 'links')
        .selectAll('g')
        .data(links)
        .enter().append('g');
    
    const linkLine = link.append('line')
        .attr('stroke-width', d => Math.sqrt(d.value) * 2)
        .attr('stroke', '#999')
        .attr('stroke-opacity', 0.6)
        .attr('marker-end', 'url(#arrow)');
    
    const linkLabel = link.append('text')
        .attr('dy', -4)
        .attr('text-anchor', 'middle')
        .attr('font-size', '8px')
        .attr('fill', '#666')
        .text(d => d.label);
    
    // Create nodes
    const node = g.append('g')
        .attr('class', 'nodes')
        .selectAll('g')
        .data(nodes)
        .enter().append('g')
        .call(d3.drag()
            .on('start', dragstarted)
            .on('drag', dragged)
            .on('end', dragended));
    
    // Add circles to nodes
    node.append('circle')
        .attr('r', d => d.size)
        .attr('fill', d => colors[d.group])
        .attr('stroke', '#fff')
        .attr('stroke-width', 1.5);
    
    // Add labels to nodes
    node.append('text')
        .attr('dx', 0)
        .attr('dy', d => d.size + 10)
        .attr('text-anchor', 'middle')
        .text(d => d.label)
        .style('font-size', '10px')
        .style('fill', '#313a46');
    
    // Add detailed info for hover tooltip
    node.append('title')
        .text(d => {
            let info = `Type: ${d.type}\n`;
            
            // Add type-specific information
            if (d.priority) info += `Priority: ${d.priority}\n`;
            if (d.status) info += `Status: ${d.status}\n`;
            if (d.subtaskCount) info += `Subtasks: ${d.subtaskCount}\n`;
            if (d.processingTime) info += `Processing Time: ${d.processingTime}\n`;
            if (d.scope) info += `Scope: ${d.scope}\n`;
            if (d.tags) info += `Tags: ${d.tags.join(', ')}\n`;
            if (d.dependencies) info += `Dependencies: ${d.dependencies}\n`;
            if (d.usage) info += `Usage: ${d.usage}\n`;
            if (d.lastTransition) info += `Last Transition: ${d.lastTransition}\n`;
            
            return info;
        });
    
    // Set up the simulation tick
    simulation.on('tick', () => {
        linkLine
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);
        
        linkLabel
            .attr('x', d => (d.source.x + d.target.x) / 2)
            .attr('y', d => (d.source.y + d.target.y) / 2);
        
        node
            .attr('transform', d => `translate(${d.x},${d.y})`);
    });
    
    // Drag functions
    function dragstarted(event, d) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }
    
    function dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }
    
    function dragended(event, d) {
        if (!event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
    
    // Add event listener to nodes for showing memory cell details
    node.on('click', function(event, d) {
        showMemoryCellDetails(d);
    });
    
    // Add legend
    const legend = svg.append('g')
        .attr('transform', 'translate(20, 20)');
    
    let legendY = 0;
    
    for (const [group, color] of Object.entries(colors)) {
        legend.append('circle')
            .attr('cx', 10)
            .attr('cy', legendY + 10)
            .attr('r', 6)
            .attr('fill', color);
        
        legend.append('text')
            .attr('x', 25)
            .attr('y', legendY + 15)
            .text(group.charAt(0).toUpperCase() + group.slice(1))
            .style('font-size', '12px')
            .style('text-transform', 'capitalize');
        
        legendY += 25;
    }
    
    // Add instructions
    svg.append('text')
        .attr('x', width - 20)
        .attr('y', height - 20)
        .attr('text-anchor', 'end')
        .text('Click on a node to inspect memory cell details')
        .style('font-size', '12px')
        .style('fill', '#6c7293');
    
    // Add zoom controls
    const zoomControls = svg.append('g')
        .attr('transform', `translate(${width - 80}, 20)`);
    
    zoomControls.append('rect')
        .attr('width', 25)
        .attr('height', 25)
        .attr('fill', '#eef2f7')
        .attr('stroke', '#d8dce5')
        .style('cursor', 'pointer')
        .on('click', () => {
            svg.transition().duration(500).call(zoom.scaleBy, 1.2);
        });
    
    zoomControls.append('text')
        .attr('x', 12.5)
        .attr('y', 17.5)
        .attr('text-anchor', 'middle')
        .text('+')
        .style('font-size', '20px')
        .style('fill', '#313a46')
        .style('cursor', 'pointer')
        .style('user-select', 'none');
    
    zoomControls.append('rect')
        .attr('width', 25)
        .attr('height', 25)
        .attr('x', 30)
        .attr('fill', '#eef2f7')
        .attr('stroke', '#d8dce5')
        .style('cursor', 'pointer')
        .on('click', () => {
            svg.transition().duration(500).call(zoom.scaleBy, 0.8);
        });
    
    zoomControls.append('text')
        .attr('x', 42.5)
        .attr('y', 17.5)
        .attr('text-anchor', 'middle')
        .text('-')
        .style('font-size', '20px')
        .style('fill', '#313a46')
        .style('cursor', 'pointer')
        .style('user-select', 'none');
}

/**
 * Create agent decision tree visualization for debugging
 */
function createAgentDecisionTree() {
    const container = document.getElementById('agent-decision-tree');
    if (!container) return;
    
    const width = container.clientWidth;
    const height = 400;
    
    // Sample decision tree data
    const data = {
        name: "Process Task",
        children: [
            {
                name: "Analyze Request",
                children: [
                    { name: "Extract Keywords", value: 15 },
                    { name: "Identify Intent", value: 20 },
                    { name: "Check Priority", value: 10 }
                ]
            },
            {
                name: "Task Decomposition",
                children: [
                    { name: "Split into Subtasks", value: 25 },
                    { name: "Assign Resources", value: 20 }
                ]
            },
            {
                name: "Process Subtasks",
                children: [
                    { 
                        name: "Subtask 1", 
                        children: [
                            { name: "Gather Data", value: 15 },
                            { name: "Analyze Data", value: 18 }
                        ]
                    },
                    { 
                        name: "Subtask 2", 
                        children: [
                            { name: "Research", value: 22 },
                            { name: "Validate", value: 15 }
                        ]
                    },
                    { 
                        name: "Subtask 3", 
                        children: [
                            { name: "Synthesize", value: 20 }
                        ]
                    }
                ]
            },
            {
                name: "Consolidate Results",
                children: [
                    { name: "Merge Results", value: 18 },
                    { name: "Generate Summary", value: 22 }
                ]
            }
        ]
    };
    
    // Create SVG element
    const svg = d3.select(container).append("svg")
        .attr("width", width)
        .attr("height", height);
    
    // Create tree layout
    const treeLayout = d3.tree().size([width - 100, height - 100]);
    
    // Create hierarchy
    const root = d3.hierarchy(data);
    
    // Assign x and y coordinates
    treeLayout(root);
    
    // Add links
    const links = svg.append("g")
        .attr("fill", "none")
        .attr("stroke", "#555")
        .attr("stroke-opacity", 0.4)
        .attr("stroke-width", 1.5)
        .selectAll("path")
        .data(root.links())
        .join("path")
        .attr("d", d3.linkHorizontal()
            .x(d => d.y + 50)  // Flip x and y for horizontal tree
            .y(d => d.x + 50));
    
    // Add nodes
    const nodes = svg.append("g")
        .selectAll("g")
        .data(root.descendants())
        .join("g")
        .attr("transform", d => `translate(${d.y + 50},${d.x + 50})`);
    
    // Node circles
    nodes.append("circle")
        .attr("fill", d => d.children ? "#4a6cf7" : "#0acf97")
        .attr("r", d => d.data.value ? Math.sqrt(d.data.value) * 1.5 : 8)
        .attr("stroke", "#fff")
        .attr("stroke-width", 2);
    
    // Node labels
    nodes.append("text")
        .attr("dy", d => d.children ? -10 : 4)
        .attr("dx", d => d.children ? 0 : 10)
        .attr("text-anchor", d => d.children ? "middle" : "start")
        .text(d => d.data.name)
        .style("font-size", "10px")
        .attr("fill", "#333");
    
    // Add title for the visualization
    svg.append("text")
        .attr("x", width / 2)
        .attr("y", 25)
        .attr("text-anchor", "middle")
        .style("font-size", "14px")
        .style("font-weight", "bold")
        .text("Agent Decision Process");
    
    // Add note
    svg.append("text")
        .attr("x", width - 20)
        .attr("y", height - 10)
        .attr("text-anchor", "end")
        .style("font-size", "10px")
        .style("font-style", "italic")
        .text("Node size represents processing time");
}

/**
 * Create performance metrics visualization for debugging
 */
function createPerformanceMetrics() {
    const container = document.getElementById('performance-metrics');
    if (!container) return;
    
    const width = container.clientWidth;
    const height = 300;
    const margin = { top: 30, right: 30, bottom: 40, left: 60 };
    const innerWidth = width - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;
    
    // Sample performance data
    const data = [
        { name: "Task Parsing", time: 50, memory: 10 },
        { name: "Decomposition", time: 120, memory: 25 },
        { name: "Subtask 1", time: 80, memory: 15 },
        { name: "Subtask 2", time: 150, memory: 30 },
        { name: "Subtask 3", time: 90, memory: 18 },
        { name: "Result Merging", time: 60, memory: 12 },
        { name: "Response Generation", time: 70, memory: 14 }
    ];
    
    // Create SVG element
    const svg = d3.select(container).append("svg")
        .attr("width", width)
        .attr("height", height);
    
    // Create group for chart
    const chart = svg.append("g")
        .attr("transform", `translate(${margin.left},${margin.top})`);
    
    // Create scales
    const xScale = d3.scaleBand()
        .domain(data.map(d => d.name))
        .range([0, innerWidth])
        .padding(0.2);
    
    const yScaleTime = d3.scaleLinear()
        .domain([0, d3.max(data, d => d.time) * 1.1])
        .range([innerHeight, 0]);
    
    const yScaleMemory = d3.scaleLinear()
        .domain([0, d3.max(data, d => d.memory) * 1.1])
        .range([innerHeight, 0]);
    
    // Create axes
    chart.append("g")
        .attr("transform", `translate(0,${innerHeight})`)
        .call(d3.axisBottom(xScale))
        .selectAll("text")
        .attr("transform", "rotate(-45)")
        .style("text-anchor", "end")
        .attr("dx", "-.8em")
        .attr("dy", ".15em")
        .style("font-size", "10px");
    
    chart.append("g")
        .call(d3.axisLeft(yScaleTime))
        .append("text")
        .attr("transform", "rotate(-90)")
        .attr("y", -40)
        .attr("x", -innerHeight / 2)
        .attr("fill", "#000")
        .attr("text-anchor", "middle")
        .text("Time (ms)");
    
    chart.append("g")
        .attr("transform", `translate(${innerWidth}, 0)`)
        .call(d3.axisRight(yScaleMemory))
        .append("text")
        .attr("transform", "rotate(-90)")
        .attr("y", 40)
        .attr("x", -innerHeight / 2)
        .attr("fill", "#000")
        .attr("text-anchor", "middle")
        .text("Memory (MB)");
    
    // Create bars for processing time
    chart.selectAll(".bar-time")
        .data(data)
        .enter().append("rect")
        .attr("class", "bar-time")
        .attr("x", d => xScale(d.name))
        .attr("y", d => yScaleTime(d.time))
        .attr("width", xScale.bandwidth() / 2)
        .attr("height", d => innerHeight - yScaleTime(d.time))
        .attr("fill", "#4a6cf7");
    
    // Create bars for memory usage
    chart.selectAll(".bar-memory")
        .data(data)
        .enter().append("rect")
        .attr("class", "bar-memory")
        .attr("x", d => xScale(d.name) + xScale.bandwidth() / 2)
        .attr("y", d => yScaleMemory(d.memory))
        .attr("width", xScale.bandwidth() / 2)
        .attr("height", d => innerHeight - yScaleMemory(d.memory))
        .attr("fill", "#fa5c7c");
    
    // Add title
    svg.append("text")
        .attr("x", width / 2)
        .attr("y", 15)
        .attr("text-anchor", "middle")
        .style("font-size", "14px")
        .style("font-weight", "bold")
        .text("Agent Performance Metrics");
    
    // Add legend
    const legend = svg.append("g")
        .attr("transform", `translate(${width - 150}, 10)`);
    
    legend.append("rect")
        .attr("width", 15)
        .attr("height", 15)
        .attr("fill", "#4a6cf7");
    
    legend.append("text")
        .attr("x", 20)
        .attr("y", 12)
        .text("Processing Time (ms)")
        .style("font-size", "10px");
    
    legend.append("rect")
        .attr("width", 15)
        .attr("height", 15)
        .attr("y", 20)
        .attr("fill", "#fa5c7c");
    
    legend.append("text")
        .attr("x", 20)
        .attr("y", 32)
        .text("Memory Usage (MB)")
        .style("font-size", "10px");
}

/**
 * Create agent state timeline for debugging
 */
function createAgentStateTimeline() {
    const container = document.getElementById('agent-state-timeline');
    if (!container) return;
    
    const width = container.clientWidth;
    const height = 120;
    const margin = { top: 30, right: 20, bottom: 20, left: 80 };
    const innerWidth = width - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;
    
    // Sample state timeline data
    const data = [
        { state: "Idle", start: 0, end: 10 },
        { state: "Processing", start: 10, end: 50 },
        { state: "Waiting", start: 50, end: 60 },
        { state: "Processing", start: 60, end: 90 },
        { state: "Finalizing", start: 90, end: 100 }
    ];
    
    // State colors
    const stateColors = {
        "Idle": "#6c7293",
        "Processing": "#4a6cf7",
        "Waiting": "#ffbc00",
        "Finalizing": "#0acf97"
    };
    
    // Create SVG element
    const svg = d3.select(container).append("svg")
        .attr("width", width)
        .attr("height", height);
    
    // Create group for chart
    const chart = svg.append("g")
        .attr("transform", `translate(${margin.left},${margin.top})`);
    
    // Create scales
    const xScale = d3.scaleLinear()
        .domain([0, 100])
        .range([0, innerWidth]);
    
    const yScale = d3.scaleBand()
        .domain(["Agent State"])
        .range([0, innerHeight])
        .padding(0.1);
    
    // Create axes
    chart.append("g")
        .attr("transform", `translate(0,${innerHeight})`)
        .call(d3.axisBottom(xScale).tickFormat(d => d + "%"));
    
    chart.append("g")
        .call(d3.axisLeft(yScale));
    
    // Create state bars
    chart.selectAll(".state-bar")
        .data(data)
        .enter().append("rect")
        .attr("class", "state-bar")
        .attr("x", d => xScale(d.start))
        .attr("y", yScale("Agent State"))
        .attr("width", d => xScale(d.end) - xScale(d.start))
        .attr("height", yScale.bandwidth())
        .attr("fill", d => stateColors[d.state])
        .append("title")
        .text(d => `${d.state}: ${d.start}% - ${d.end}%`);
    
    // Add state labels
    chart.selectAll(".state-label")
        .data(data)
        .enter().append("text")
        .attr("class", "state-label")
        .attr("x", d => xScale(d.start) + (xScale(d.end) - xScale(d.start)) / 2)
        .attr("y", yScale("Agent State") + yScale.bandwidth() / 2 + 5)
        .attr("text-anchor", "middle")
        .style("font-size", "10px")
        .style("fill", "#fff")
        .style("font-weight", "bold")
        .text(d => d.state);
    
    // Add title
    svg.append("text")
        .attr("x", width / 2)
        .attr("y", 15)
        .attr("text-anchor", "middle")
        .style("font-size", "14px")
        .style("font-weight", "bold")
        .text("Agent State Timeline");
}

/**
 * Show memory cell details
 */
function showMemoryCellDetails(cell) {
    // Hide no selection message
    document.querySelector('.no-selection')?.classList.add('hidden');
    
    // Show cell details
    document.querySelector('.cell-details')?.classList.remove('hidden');
    
    // Populate cell details if the elements exist
    document.getElementById('cell-id')?.textContent && (document.getElementById('cell-id').textContent = cell.id);
    document.getElementById('cell-tags')?.textContent && (document.getElementById('cell-tags').textContent = cell.group);
    document.getElementById('cell-created')?.textContent && (document.getElementById('cell-created').textContent = new Date().toLocaleString());
    document.getElementById('cell-modified')?.textContent && (document.getElementById('cell-modified').textContent = new Date().toLocaleString());
    document.getElementById('cell-accessed')?.textContent && (document.getElementById('cell-accessed').textContent = new Date().toLocaleString());
    document.getElementById('cell-size')?.textContent && (document.getElementById('cell-size').textContent = `${cell.size} KB`);
    
    // Populate example value based on cell group
    let value = '';
    
    switch (cell.group) {
        case 'request':
            value = JSON.stringify({
                id: "task-1001",
                title: "Research quantum computing",
                description: "Investigate quantum computing applications",
                priority: "High",
                tags: ["research", "quantum", "computing"]
            }, null, 2);
            break;
        case 'response':
            value = JSON.stringify({
                requestId: "task-1001",
                status: "Completed",
                summary: "Task completed successfully",
                results: [
                    { subtaskId: "st-1", status: "Completed", output: "Analysis of quantum platforms completed" },
                    { subtaskId: "st-2", status: "Completed", output: "Evaluation of quantum ML applications completed" },
                    { subtaskId: "st-3", status: "Completed", output: "Investigation of quantum computing state completed" }
                ]
            }, null, 2);
            break;
        case 'subtask':
            value = JSON.stringify({
                id: "st-" + cell.id.substring(4),
                parentTaskId: "task-1001",
                title: "Subtask " + cell.id.substring(4),
                description: "Process subtask data",
                status: "Completed",
                assignedAgent: "TaskProcessorAgent",
                metadata: {
                    processingTime: "0.5s",
                    priority: "High"
                }
            }, null, 2);
            break;
        case 'result':
            value = JSON.stringify({
                subtaskId: "st-" + cell.id.substring(4),
                status: "Completed",
                output: "Processed data for subtask " + cell.id.substring(4),
                error: null,
                metadata: {
                    confidence: 0.95,
                    sources: ["source1", "source2"]
                }
            }, null, 2);
            break;
        case 'context':
            value = JSON.stringify({
                type: "Context",
                metadata: {
                    sessionId: "session-123",
                    timestamp: new Date().toISOString(),
                    scope: "Global"
                },
                data: {
                    systemPrompt: "You are a helpful assistant specialized in quantum computing research...",
                    conversationHistory: [
                        { role: "user", content: "Tell me about quantum computing applications" }
                    ],
                    references: [
                        { title: "Quantum Computing Basics", url: "https://example.com/quantum1" },
                        { title: "Quantum ML Applications", url: "https://example.com/quantum2" }
                    ]
                }
            }, null, 2);
            break;
        case 'system':
            value = JSON.stringify({
                type: "SystemConfig",
                agentSettings: {
                    maxConcurrentTasks: 5,
                    timeoutSeconds: 30,
                    memoryCompression: true,
                    loggingLevel: "info"
                },
                serviceConnections: {
                    database: "connected",
                    messaging: "connected",
                    storage: "connected"
                }
            }, null, 2);
            break;
        case 'agent':
            value = JSON.stringify({
                type: "AgentState",
                id: "agent-1",
                name: "TaskProcessorAgent",
                status: "Active",
                currentTasks: 1,
                completedTasks: 42,
                uptime: "2h 15m",
                lastTransition: {
                    from: "Idle",
                    to: "Processing",
                    timestamp: new Date().toISOString()
                }
            }, null, 2);
            break;
    }
    
    // Update cell value if element exists
    document.getElementById('cell-value')?.textContent && (document.getElementById('cell-value').textContent = value);
}

/**
 * Task form handling
 */
function initializeTaskForm() {
    const taskForm = document.getElementById('task-form');
    
    if (taskForm) {
        taskForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Get form values
            const title = document.getElementById('task-title').value;
            const description = document.getElementById('task-description').value;
            const priority = document.getElementById('task-priority').value;
            const tagsInput = document.getElementById('task-tags').value;
            const tags = tagsInput.split(',').map(tag => tag.trim()).filter(tag => tag);
            
            // Create task object
            const task = {
                id: 'T-' + Math.floor(1000 + Math.random() * 9000),
                title,
                description,
                priority,
                tags: tags,
                createdAt: new Date().toISOString()
            };
            
            // In a real implementation, we would send this to the server
            console.log('Submitting task:', task);
            
            // For demo purposes, add to the task list
            addTaskToList(task);
            
            // Reset form
            taskForm.reset();
            
            // Show success message
            alert('Task submitted successfully!');
        });
    }
}

/**
 * Add a task to the task list
 */
function addTaskToList(task) {
    const taskList = document.querySelector('.task-list-container tbody');
    if (!taskList) return;
    
    const row = document.createElement('tr');
    row.innerHTML = `
        <td>${task.id}</td>
        <td>${task.title}</td>
        <td>${task.priority}</td>
        <td><span class="status pending">Pending</span></td>
        <td>${new Date().toLocaleString()}</td>
        <td>
            <button class="action-btn view-btn" data-task-id="${task.id}">View</button>
        </td>
    `;
    
    taskList.prepend(row);
    
    // Attach event listener to view button
    row.querySelector('.view-btn')?.addEventListener('click', function() {
        showTaskDetails(task);
    });
}

/**
 * Show task details in modal
 */
function showTaskDetails(task) {
    const modal = document.getElementById('task-detail-modal');
    if (!modal) return;
    
    // Populate task details
    document.getElementById('detail-task-id')?.textContent && (document.getElementById('detail-task-id').textContent = task.id);
    document.getElementById('detail-task-title')?.textContent && (document.getElementById('detail-task-title').textContent = task.title);
    document.getElementById('detail-task-description')?.textContent && (document.getElementById('detail-task-description').textContent = task.description);
    document.getElementById('detail-task-status')?.textContent && (document.getElementById('detail-task-status').textContent = 'Pending');
    document.getElementById('detail-task-priority')?.textContent && (document.getElementById('detail-task-priority').textContent = task.priority);
    document.getElementById('detail-task-created')?.textContent && (document.getElementById('detail-task-created').textContent = new Date().toLocaleString());
    document.getElementById('detail-task-completed')?.textContent && (document.getElementById('detail-task-completed').textContent = 'N/A');
    
    // Clear subtasks
    const subtasksList = document.getElementById('subtasks-list');
    if (subtasksList) {
        subtasksList.innerHTML = '';
        
        // For demo, generate some subtasks
        const numSubtasks = task.description.split('\n\n').filter(p => p.trim()).length || 3;
        for (let i = 1; i <= numSubtasks; i++) {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>ST-${i}</td>
                <td>${task.title} - Part ${i}</td>
                <td><span class="status pending">Pending</span></td>
                <td>Waiting to be processed...</td>
            `;
            subtasksList.appendChild(row);
            
            // Simulate processing after a delay
            setTimeout(() => {
                if (subtasksList.contains(row)) {
                    row.querySelector('.status').className = 'status in-progress';
                    row.querySelector('.status').textContent = 'In Progress';
                    row.cells[3].textContent = 'Processing...';
                }
            }, 2000 + i * 1000);
            
            setTimeout(() => {
                if (subtasksList.contains(row)) {
                    row.querySelector('.status').className = 'status completed';
                    row.querySelector('.status').textContent = 'Completed';
                    row.cells[3].textContent = `Processed subtask ${i} successfully`;
                }
            }, 5000 + i * 2000);
        }
    }
    
    // Set summary
    document.getElementById('task-summary')?.textContent && (document.getElementById('task-summary').textContent = 
        `Task '${task.title}' submitted with ${subtasksList ? subtasksList.childElementCount : 3} subtasks. Processing will begin shortly.`);
    
    // Show modal
    modal.style.display = 'block';
    
    // Close button event
    modal.querySelector('.close-btn')?.addEventListener('click', function() {
        modal.style.display = 'none';
    });
    
    // Close when clicking outside
    window.addEventListener('click', function(event) {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}

/**
 * WebSocket connection for real-time updates
 */
function initializeWebSocket() {
    // In a real implementation, this would connect to the server's WebSocket
    // For demo purposes, we'll simulate updates
    
    // Simulate log updates
    setInterval(function() {
        const logContainer = document.querySelector('.log-container');
        if (logContainer) {
            const logTypes = ['info', 'debug', 'warning', 'error'];
            const logType = logTypes[Math.floor(Math.random() * logTypes.length)];
            const messages = [
                'Agent state updated',
                'Memory cell accessed',
                'Task status changed',
                'System health check completed',
                'Configuration loaded',
                'Cache optimized',
                'Memory compression initiated',
                'Subtask completed',
                'Decision point reached',
                'API request received',
                'Response generated',
                'Memory cell created',
                'Context updated'
            ];
            const message = messages[Math.floor(Math.random() * messages.length)];
            
            const now = new Date();
            const timestamp = now.getHours().toString().padStart(2, '0') + ':' +
                             now.getMinutes().toString().padStart(2, '0') + ':' +
                             now.getSeconds().toString().padStart(2, '0');
            
            const logEntry = document.createElement('div');
            logEntry.className = `log-entry ${logType}`;
            logEntry.innerHTML = `
                <span class="timestamp">${timestamp}</span>
                <span class="log-text">${message}</span>
            `;
            
            logContainer.prepend(logEntry);
            
            // Keep only the last 100 log entries
            const logEntries = logContainer.querySelectorAll('.log-entry');
            if (logEntries.length > 100) {
                logContainer.removeChild(logEntries[logEntries.length - 1]);
            }
        }
    }, 3000);
    
    // Simulate agent status changes
    setInterval(function() {
        const agents = document.querySelectorAll('.agent');
        if (agents.length > 0) {
            const randomAgent = agents[Math.floor(Math.random() * agents.length)];
            const stateElement = randomAgent.querySelector('.agent-state');
            
            // Get current state
            const currentState = stateElement.textContent.trim();
            
            // Possible state transitions
            const transitions = {
                'Active': ['Idle', 'Active'],
                'Idle': ['Active', 'Idle']
            };
            
            // Get possible next states
            const possibleStates = transitions[currentState] || ['Active', 'Idle'];
            
            // Select a random next state
            const nextState = possibleStates[Math.floor(Math.random() * possibleStates.length)];
            
            // Update state
            stateElement.textContent = nextState;
            stateElement.className = `agent-state ${nextState.toLowerCase()}`;
            
            // Update metadata
            const metadataElement = randomAgent.querySelector('.agent-metadata');
            const memoryUsage = Math.floor(Math.random() * 30) + 5;
            metadataElement.textContent = `Memory Usage: ${memoryUsage}MB`;
        }
    }, 10000);
}

/**
 * Memory inspector functionality
 */
function initializeMemoryInspector() {
    const searchButton = document.getElementById('search-btn');
    const searchInput = document.getElementById('memory-search');
    const tagFilter = document.getElementById('tag-filter');
    
    if (searchButton && searchInput) {
        searchButton.addEventListener('click', function() {
            const searchTerm = searchInput.value.trim();
            console.log('Searching for:', searchTerm);
            // In a real implementation, this would filter the memory cells
            
            // For demo, add this functionality
            const nodes = document.querySelectorAll('.nodes circle');
            if (nodes.length > 0) {
                // Reset all nodes
                nodes.forEach(node => {
                    node.setAttribute('stroke-width', '1.5');
                    node.setAttribute('stroke', '#fff');
                });
                
                if (searchTerm) {
                    // Highlight nodes containing the search term
                    nodes.forEach(node => {
                        const parentG = node.parentElement;
                        const title = parentG.querySelector('title');
                        if (title && title.textContent.toLowerCase().includes(searchTerm.toLowerCase())) {
                            node.setAttribute('stroke-width', '3');
                            node.setAttribute('stroke', '#fa5c7c');
                        }
                    });
                }
            }
        });
    }
    
    if (tagFilter) {
        tagFilter.addEventListener('change', function() {
            const selectedTag = this.value;
            console.log('Filtering by tag:', selectedTag);
            // In a real implementation, this would filter the memory cells
            
            // For demo, add this functionality
            const nodes = document.querySelectorAll('.nodes circle');
            if (nodes.length > 0) {
                // Reset all nodes
                nodes.forEach(node => {
                    node.parentElement.style.opacity = '1';
                });
                
                if (selectedTag) {
                    // Dim nodes not matching the tag
                    nodes.forEach(node => {
                        const parentG = node.parentElement;
                        const group = parentG.__data__.group;
                        if (group !== selectedTag) {
                            parentG.style.opacity = '0.2';
                        }
                    });
                }
            }
        });
    }
}

/**
 * Documentation navigation
 */
function initializeDocumentationNavigation() {
    const docLinks = document.querySelectorAll('.docs-nav a');
    const docSections = document.querySelectorAll('.doc-section');
    
    docLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all links and sections
            docLinks.forEach(link => link.classList.remove('active'));
            docSections.forEach(section => section.classList.remove('active'));
            
            // Add active class to clicked link
            this.classList.add('active');
            
            // Show the corresponding section
            const sectionId = this.getAttribute('href').substring(1);
            document.getElementById(sectionId)?.classList.add('active');
        });
    });
}
