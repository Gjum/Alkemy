import React from 'react';
import Config from './Config';
import ContainersVis from './ContainersVis';

function App() {
	const containers = [
		{
			type: 'Canister', name: 'Storage Canister',
			maxVolume: 100, liquidVolume: 80, temperature: 300, mass: 80,
		},
		{
			type: 'Canister', name: 'Buffer Canister',
			maxVolume: 100, liquidVolume: 30, temperature: 300, mass: 30,
		},
		{
			type: 'Flask', name: 'Result Flask',
			maxVolume: 100, liquidVolume: 20, temperature: 300, mass: 20,
		},
	]
	const wiringConfig = [
		['Storage Canister', 'gas', 'Buffer Canister'],
		['Storage Canister', 'liquid', 'Buffer Canister'],
		['Buffer Canister', 'liquid', 'Result Flask'],
		['Buffer Canister', 'gas', 'Result Flask'],
	]
	const wiring = {}
	wiringConfig.forEach(([source, medium, sink]) => {
		wiring[source] = { ...wiring[source], [medium + 'Out']: sink }
		wiring[sink] = { ...wiring[sink], [medium + 'In']: source }
	})

	return (
		<div className='App'>
			<Config />
			<ContainersVis containers={containers} wiring={wiring} />
		</div>
	);
}

export default App;
